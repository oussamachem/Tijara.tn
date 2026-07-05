package com.smartboutique.service;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SalePaymentRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.entity.PaymentMethod;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.entity.Sale;
import com.smartboutique.entity.SaleItem;
import com.smartboutique.entity.SalePayment;
import com.smartboutique.entity.TenderMethod;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.SaleMapper;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SalePaymentRepository;
import com.smartboutique.repository.SaleRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Gestion des ventes (au grain VARIANTE depuis la Phase 9).
 *
 * <p><b>Transaction (tout ou rien)</b> : vente + lignes + decrement du stock VARIANTE dans une
 * seule transaction ; toute erreur provoque un rollback complet.</p>
 *
 * <p><b>Concurrence</b> : decrement atomique conditionnel sur {@code product_variant.quantity}
 * ({@code UPDATE ... WHERE quantity >= :q}). 0 ligne affectee => stock insuffisant (419/409),
 * pas de survente. Lignes triees par id variante pour limiter les interblocages.</p>
 *
 * <p><b>Prix</b> : prix unitaire = prix du PRODUIT, capture a la vente. Couleur/taille/reference
 * variante sont denormalises sur la ligne pour figer l'historique.</p>
 *
 * <p><b>Remise</b> : montant fixe soustrait du sous-total ; total negatif refuse.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final SalePaymentRepository salePaymentRepository;
    private final SaleMapper saleMapper;

    /** Denominations autorisees pour les tickets cadeaux (constante configurable, MVP). */
    @Value("${app.gift-ticket-denominations:10,20,50}")
    private String denominationsCsv;

    @Transactional
    public SaleResponse createSale(SaleRequest request, Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendeur", sellerId));

        Sale sale = Sale.builder()
                .seller(seller)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;

        // Tri par id variante pour reduire le risque d'interblocage entre ventes concurrentes.
        List<SaleItemRequest> items = request.items().stream()
                .sorted(Comparator.comparing(SaleItemRequest::variantId))
                .toList();

        for (SaleItemRequest line : items) {
            ProductVariant variant = variantRepository.findById(line.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variante", line.variantId()));

            // Decrement atomique conditionnel de la VARIANTE : anti-survente garanti par la BDD.
            int updated = variantRepository.decrementStockIfAvailable(variant.getId(), line.quantity());
            if (updated == 0) {
                throw new BusinessException(
                        "Stock insuffisant pour la declinaison '" + variant.getReference()
                                + "' (" + variant.getColor().getName() + " / taille " + variant.getSize().getLabel()
                                + ") : demande " + line.quantity() + ", disponible " + variant.getQuantity(),
                        HttpStatus.CONFLICT);
            }

            // Capture du prix (prix produit) + denormalisation des attributs variante.
            BigDecimal unitPrice = variant.getProduct().getSalePrice();
            BigDecimal linePrice = unitPrice.multiply(BigDecimal.valueOf(line.quantity()));

            SaleItem saleItem = SaleItem.builder()
                    .variant(variant)
                    .quantity(line.quantity())
                    .unitPrice(unitPrice)
                    .totalPrice(linePrice)
                    .variantReference(variant.getReference())
                    .productName(variant.getProduct().getName())
                    .colorName(variant.getColor().getName())
                    .size(variant.getSize().getLabel())
                    .build();
            sale.addItem(saleItem);

            subtotal = subtotal.add(linePrice);
        }

        BigDecimal discount = request.discount() != null ? request.discount() : BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(
                    "La remise (" + discount + ") est superieure au sous-total (" + subtotal + ")",
                    HttpStatus.BAD_REQUEST);
        }

        sale.setDiscount(discount);
        sale.setTotalAmount(total);

        // Paiement : liste de tenders (especes/carte/ticket cadeau) validee et figee sur la vente.
        applyTenders(sale, request, total);

        Sale saved = saleRepository.save(sale);
        log.info("Vente id={} enregistree par {} : {} ligne(s), total {}, {} tender(s)",
                saved.getId(), seller.getEmail(), saved.getItems().size(), total, saved.getPayments().size());
        return saleMapper.toResponse(saved);
    }

    /**
     * Valide et attache les tenders. Le stock et le CA restent inchanges : le ticket cadeau
     * n'est qu'un mode de reglement (CA = prix des articles).
     */
    private void applyTenders(Sale sale, SaleRequest request, BigDecimal total) {
        List<SalePaymentRequest> tenders = request.payments();

        // Retro-compatibilite : pas de liste -> un seul tender deduit du paymentMethod.
        if (tenders == null || tenders.isEmpty()) {
            PaymentMethod pm = request.paymentMethod();
            if (pm == null) {
                throw new BusinessException("Mode de paiement obligatoire (paymentMethod ou payments)",
                        HttpStatus.BAD_REQUEST);
            }
            sale.setPaymentMethod(pm);
            sale.addPayment(SalePayment.builder()
                    .method(pm == PaymentMethod.CARTE ? TenderMethod.CARTE : TenderMethod.ESPECES)
                    .amount(total).build());
            return;
        }

        Set<String> codesInSale = new HashSet<>();
        BigDecimal sum = BigDecimal.ZERO;
        for (SalePaymentRequest t : tenders) {
            sum = sum.add(t.amount());
            if (t.method() == TenderMethod.TICKET_CADEAU) {
                validateGiftTicket(t, codesInSale);
            }
            sale.addPayment(SalePayment.builder()
                    .method(t.method()).amount(t.amount())
                    .issuer(t.issuer()).ticketCode(t.ticketCode())
                    .ticketSerial(t.ticketSerial()).ticketExpiry(t.ticketExpiry())
                    .build());
        }
        if (sum.compareTo(total) < 0) {
            throw new BusinessException(
                    "Paiements insuffisants : total des tenders " + sum + " < total a payer " + total,
                    HttpStatus.BAD_REQUEST);
        }
        sale.setPaymentMethod(rollup(tenders));
    }

    private void validateGiftTicket(SalePaymentRequest t, Set<String> codesInSale) {
        if (t.issuer() == null) {
            throw new BusinessException("Emetteur du ticket cadeau obligatoire", HttpStatus.BAD_REQUEST);
        }
        if (t.ticketCode() == null || t.ticketCode().isBlank()) {
            throw new BusinessException("Code du ticket cadeau obligatoire", HttpStatus.BAD_REQUEST);
        }
        if (!allowedDenominations().stream().anyMatch(d -> d.compareTo(t.amount()) == 0)) {
            throw new BusinessException(
                    "Denomination de ticket invalide : " + t.amount() + " (autorisees : " + denominationsCsv + ")",
                    HttpStatus.BAD_REQUEST);
        }
        if (t.ticketExpiry() != null && t.ticketExpiry().isBefore(LocalDate.now())) {
            throw new BusinessException("Ticket cadeau expire (" + t.ticketExpiry() + ")", HttpStatus.BAD_REQUEST);
        }
        if (!codesInSale.add(t.ticketCode())) {
            throw new BusinessException("Ticket cadeau en double dans la vente : " + t.ticketCode(),
                    HttpStatus.BAD_REQUEST);
        }
        // Usage unique GLOBAL (A4) : deja encaisse sur une autre vente -> 409.
        if (salePaymentRepository.existsByMethodAndTicketCode(TenderMethod.TICKET_CADEAU, t.ticketCode())) {
            throw new BusinessException("Ticket cadeau deja utilise : " + t.ticketCode(), HttpStatus.CONFLICT);
        }
    }

    private List<BigDecimal> allowedDenominations() {
        return Arrays.stream(denominationsCsv.split(",")).map(String::trim)
                .filter(s -> !s.isEmpty()).map(BigDecimal::new).toList();
    }

    /** Rollup vers le paymentMethod de la vente (colonne existante) : MIXTE si ticket ou melange. */
    private PaymentMethod rollup(List<SalePaymentRequest> tenders) {
        Set<TenderMethod> methods = tenders.stream().map(SalePaymentRequest::method)
                .collect(java.util.stream.Collectors.toSet());
        if (methods.equals(Set.of(TenderMethod.ESPECES))) return PaymentMethod.ESPECES;
        if (methods.equals(Set.of(TenderMethod.CARTE))) return PaymentMethod.CARTE;
        return PaymentMethod.MIXTE;
    }

    /** Detail d'une vente (lignes denormalisees + vendeur), charge sans N+1 via @EntityGraph. */
    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", id));
        return saleMapper.toResponse(sale);
    }

    /** Historique pagine des ventes, filtres optionnels par periode (dates incluses) et vendeur. */
    @Transactional(readOnly = true)
    public PageResponse<SaleSummaryResponse> searchHistory(LocalDate from, LocalDate to,
                                                           Long sellerId, Pageable pageable) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;
        Page<SaleSummaryResponse> page = saleRepository.searchHistory(start, end, sellerId, pageable);
        return PageResponse.of(page, page.getContent());
    }
}
