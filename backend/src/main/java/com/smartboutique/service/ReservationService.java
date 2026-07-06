package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.*;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.ReservationMapper;
import com.smartboutique.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Reservations (layaway). Le produit est RETENU en boutique : le stock est decremente des la
 * creation (verrou variante conditionnel, meme anti-survente que les ventes) et rendu a
 * l'expiration/annulation. L'acompte est un DEPOT (passif) ; le CA n'est reconnu qu'a la CLOTURE
 * (COMPLETED) en creant la vente de reference SANS re-decrementer le stock.
 *
 * <p>Toutes les operations sont transactionnelles (tout ou rien). Montants BigDecimal, autoritatifs
 * cote serveur (le client n'envoie jamais le total).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationPaymentRepository paymentRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final SaleRepository saleRepository;
    private final ReservationMapper mapper;

    /** Duree par defaut d'une reservation (B4, jours). */
    @Value("${app.reservations.default-duration-days:30}")
    private int defaultDurationDays;

    /** Seuil d'alerte "echeance proche" (B5, jours). */
    @Value("${app.reservations.reminder-days:4}")
    private int reminderDays;

    // ------------------------------------- Creation -------------------------------------

    @Transactional
    public ReservationResponse create(ReservationCreateRequest request, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendeur", sellerId));

        int duration = request.durationDays() != null ? request.durationDays() : defaultDurationDays;
        if (duration <= 0) {
            throw new BusinessException("La duree doit etre strictement positive (jours)", HttpStatus.BAD_REQUEST);
        }

        Reservation reservation = Reservation.builder()
                .reference("TMP-" + UUID.randomUUID())   // placeholder unique, remplace apres l'insert
                .customerName(request.customerName().trim())
                .customerPhone(request.customerPhone())
                .seller(seller)
                .status(ReservationStatus.ACTIVE)
                .dueDate(LocalDateTime.now().plusDays(duration))
                .build();

        BigDecimal total = BigDecimal.ZERO;
        // Tri par id variante pour reduire le risque d'interblocage entre operations concurrentes.
        List<ReservationItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(ReservationItemRequest::variantId))
                .toList();

        for (ReservationItemRequest line : items) {
            ProductVariant variant = variantRepository.findById(line.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante", line.variantId()));

            // Retenue de stock : decrement atomique conditionnel (anti-survente garanti par la BDD).
            int updated = variantRepository.decrementStockIfAvailable(variant.getId(), line.quantity());
            if (updated == 0) {
                throw new BusinessException(
                        "Stock insuffisant pour la declinaison '" + variant.getReference()
                                + "' (" + variant.getColor().getName() + " / taille " + variant.getSize().getLabel()
                                + ") : demande " + line.quantity() + ", disponible " + variant.getQuantity(),
                        HttpStatus.CONFLICT);
            }

            BigDecimal unitPrice = variant.getProduct().getSalePrice();
            reservation.addItem(ReservationItem.builder()
                    .variant(variant)
                    .quantity(line.quantity())
                    .unitPrice(unitPrice)
                    .variantReference(variant.getReference())
                    .productName(variant.getProduct().getName())
                    .colorName(variant.getColor().getName())
                    .size(variant.getSize().getLabel())
                    .build());
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(line.quantity())));
        }
        reservation.setTotalAmount(total);

        // Acompte initial (optionnel) = premier versement ; ne peut depasser le total.
        BigDecimal down = request.downPayment() != null ? request.downPayment() : BigDecimal.ZERO;
        if (down.signum() > 0) {
            if (down.compareTo(total) > 0) {
                throw new BusinessException(
                        "L'acompte (" + down + ") ne peut depasser le total (" + total + ")",
                        HttpStatus.BAD_REQUEST);
            }
            reservation.addPayment(ReservationPayment.builder()
                    .amount(down).method(parseMethod(request.downPaymentMethod())).seller(seller).build());
        }

        reservation = reservationRepository.save(reservation);
        reservation.setReference(reference(reservation.getId()));

        // Solde complet des la creation -> cloture immediate (CA reconnu).
        if (down.signum() > 0 && down.compareTo(total) >= 0) {
            recognizeRevenue(reservation);
        }
        reservationRepository.save(reservation);

        log.info("Reservation {} creee par {} : {} ligne(s), total {}, acompte {}, echeance {}",
                reservation.getReference(), seller.getEmail(), reservation.getItems().size(),
                total, down, reservation.getDueDate());
        return mapper.toResponse(reservation, down);
    }

    // ------------------------------------- Paiement -------------------------------------

    @Transactional
    public ReservationResponse addPayment(Long reservationId, ReservationPaymentRequest request, Long sellerId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException(
                    "Reservation " + reservation.getStatus() + " : aucun versement possible", HttpStatus.CONFLICT);
        }
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendeur", sellerId));

        BigDecimal total = reservation.getTotalAmount();
        BigDecimal paid = paymentRepository.sumPaidByReservation(reservationId);
        BigDecimal remaining = total.subtract(paid);
        if (request.amount().compareTo(remaining) > 0) {
            throw new BusinessException(
                    "Versement (" + request.amount() + ") superieur au reste du (" + remaining + ")",
                    HttpStatus.BAD_REQUEST);
        }

        // Persistance directe du versement (pas via la collection lazy d'une entite rechargee :
        // eviterait un TransientObjectException au flush avec orphanRemoval).
        paymentRepository.save(ReservationPayment.builder()
                .reservation(reservation).amount(request.amount()).method(request.method()).seller(seller).build());
        BigDecimal newPaid = paid.add(request.amount());

        // Solde -> cloture (CA reconnu ici, B3).
        if (newPaid.compareTo(total) >= 0) {
            recognizeRevenue(reservation);
        }
        reservationRepository.save(reservation);
        log.info("Versement {} sur reservation {} (paye {} / {}){}",
                request.amount(), reservation.getReference(), newPaid, total,
                reservation.getStatus() == ReservationStatus.COMPLETED ? " -> SOLDEE" : "");
        return mapper.toResponse(reservation, newPaid);
    }

    // ------------------------------------- Annulation -------------------------------------

    /** Annulation : rend le stock retenu ; l'acompte deja verse est retenu + flagge (B6). */
    @Transactional
    public ReservationResponse cancel(Long reservationId) {
        Reservation reservation = getReservation(reservationId);
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new BusinessException(
                    "Reservation deja " + reservation.getStatus(), HttpStatus.CONFLICT);
        }
        BigDecimal paid = paymentRepository.sumPaidByReservation(reservationId);
        releaseStock(reservation);
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setClosedAt(LocalDateTime.now());
        if (paid.signum() > 0) reservation.setDepositForfeited(true);   // B6
        reservationRepository.save(reservation);
        log.warn("[AUDIT] Reservation {} annulee (stock rendu, acompte retenu {})",
                reservation.getReference(), paid);
        return mapper.toResponse(reservation, paid);
    }

    // ------------------------------------- Expiration -------------------------------------

    /**
     * Job quotidien : passe en EXPIRED toutes les reservations ACTIVE dont l'echeance est depassee,
     * rend leur stock et flagge l'acompte retenu (B6). Renvoie le nombre de reservations expirees.
     */
    @Transactional
    public int expireOverdue() {
        List<Reservation> overdue = reservationRepository
                .findByStatusAndDueDateBefore(ReservationStatus.ACTIVE, LocalDateTime.now());
        for (Reservation reservation : overdue) {
            BigDecimal paid = paymentRepository.sumPaidByReservation(reservation.getId());
            releaseStock(reservation);
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservation.setClosedAt(LocalDateTime.now());
            if (paid.signum() > 0) reservation.setDepositForfeited(true);   // B6
            reservationRepository.save(reservation);
            log.warn("[AUDIT] Reservation {} expiree (echeance {}, stock rendu, acompte retenu {})",
                    reservation.getReference(), reservation.getDueDate(), paid);
        }
        if (!overdue.isEmpty()) log.info("Expiration reservations : {} passee(s) en EXPIRED", overdue.size());
        return overdue.size();
    }

    // --------------------------------- Lecture / recherche ---------------------------------

    @Transactional(readOnly = true)
    public ReservationResponse findById(Long id) {
        Reservation reservation = reservationRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        return mapper.toResponse(reservation, paymentRepository.sumPaidByReservation(id));
    }

    @Transactional(readOnly = true)
    public List<ReservationSummaryResponse> list(String status) {
        List<ReservationRow> rows = (status == null || status.isBlank())
                ? reservationRepository.findAllRows()
                : reservationRepository.findRowsByStatus(parseStatus(status));
        return rows.stream().map(mapper::toSummary).toList();
    }

    /** Echeance proche (alerte vendeur B5) : ACTIVE dont l'echeance <= now + reminderDays. */
    @Transactional(readOnly = true)
    public List<ReservationSummaryResponse> dueSoon() {
        LocalDateTime threshold = LocalDateTime.now().plusDays(reminderDays);
        return reservationRepository.findDueSoon(ReservationStatus.ACTIVE, threshold)
                .stream().map(mapper::toSummary).toList();
    }

    // ------------------------------------- Interne -------------------------------------

    /**
     * Reconnaissance du CA a la cloture (B3) : cree la vente de reference (lignes + tenders repris
     * des versements) SANS re-decrementer le stock (deja retenu a la creation), puis passe la
     * reservation en COMPLETED. Idempotent-safe : n'agit que sur une reservation encore ACTIVE.
     */
    private void recognizeRevenue(Reservation reservation) {
        Sale sale = Sale.builder().seller(reservation.getSeller()).build();
        sale.setDiscount(BigDecimal.ZERO);
        sale.setTotalAmount(reservation.getTotalAmount());
        for (ReservationItem it : reservation.getItems()) {
            BigDecimal linePrice = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
            sale.addItem(SaleItem.builder()
                    .variant(it.getVariant())
                    .quantity(it.getQuantity())
                    .unitPrice(it.getUnitPrice())
                    .totalPrice(linePrice)
                    .variantReference(it.getVariantReference())
                    .productName(it.getProductName())
                    .colorName(it.getColorName())
                    .size(it.getSize())
                    .build());
        }
        // Tenders repris des versements (requete : robuste que le versement vienne de la collection
        // de creation ou d'un addPayment persiste directement).
        List<ReservationPayment> tenders = paymentRepository.findByReservationIdOrderByCreatedAtAsc(reservation.getId());
        for (ReservationPayment p : tenders) {
            sale.addPayment(SalePayment.builder().method(p.getMethod()).amount(p.getAmount()).build());
        }
        sale.setPaymentMethod(rollup(tenders));
        Sale saved = saleRepository.save(sale);

        reservation.setSale(saved);
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setClosedAt(LocalDateTime.now());
        log.info("[AUDIT] Reservation {} SOLDEE -> vente #{} (CA reconnu {})",
                reservation.getReference(), saved.getId(), reservation.getTotalAmount());
    }

    /** Rend le stock retenu de chaque ligne (expiration/annulation). */
    private void releaseStock(Reservation reservation) {
        for (ReservationItem it : reservation.getItems()) {
            variantRepository.incrementStock(it.getVariant().getId(), it.getQuantity());
        }
    }

    /** Roll-up des tenders vers le paymentMethod de la vente : MIXTE si melange ou ticket. */
    private PaymentMethod rollup(List<ReservationPayment> payments) {
        Set<TenderMethod> methods = payments.stream()
                .map(ReservationPayment::getMethod).collect(Collectors.toSet());
        if (methods.equals(Set.of(TenderMethod.ESPECES))) return PaymentMethod.ESPECES;
        if (methods.equals(Set.of(TenderMethod.CARTE))) return PaymentMethod.CARTE;
        return PaymentMethod.MIXTE;
    }

    private TenderMethod parseMethod(String method) {
        if (method == null || method.isBlank()) return TenderMethod.ESPECES;
        try {
            return TenderMethod.valueOf(method.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Mode de paiement invalide : " + method, HttpStatus.BAD_REQUEST);
        }
    }

    private ReservationStatus parseStatus(String status) {
        try {
            return ReservationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Statut invalide : " + status, HttpStatus.BAD_REQUEST);
        }
    }

    private String reference(Long id) {
        return "RES-" + String.format("%06d", id);
    }

    private Reservation getReservation(Long id) {
        return reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
    }
}
