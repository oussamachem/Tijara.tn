package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.*;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.CreditMapper;
import com.smartboutique.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ventes A CREDIT. On REUTILISE la creation de vente (verrou variante Phase 4) pour sortir le
 * stock ; on AJOUTE la couche client / acompte / paiements par-dessus. Reconnaissance du CA =
 * accrual (la vente compte en plein le jour J ; dashboard ventes inchange). Annulation = restock
 * + reversal (retours) tracable, qui corrige le CA net via le mecanisme de retour existant.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditRepository creditRepository;
    private final CreditPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final SaleRepository saleRepository;
    private final ReturnRepository returnRepository;
    private final ProductVariantRepository variantRepository;
    private final SaleService saleService;
    private final CreditMapper creditMapper;

    // ------------------------------------- Creation -------------------------------------

    @Transactional
    public CreditResponse create(CreditCreateRequest request, Long adminId) {
        Customer customer = resolveCustomer(request);

        // 1) Cree la VENTE (lignes + prix captures + decrement stock via le verrou variante).
        //    Stock insuffisant -> 409 leve par SaleService -> rollback de TOUT (rien n'est cree).
        PaymentMethod method = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.ESPECES;
        SaleResponse sale = saleService.createSale(
                new SaleRequest(request.items(), method, request.discount(), null), adminId);
        Sale saleEntity = saleRepository.findById(sale.id())
                .orElseThrow(() -> new ResourceNotFoundException("Vente", sale.id()));

        // 2) Cree le CREDIT (1-1 avec la vente).
        Credit credit = Credit.builder()
                .sale(saleEntity)
                .customer(customer)
                .dueDate(request.dueDate())
                .cancelled(false)
                .build();

        // 3) Acompte initial (optionnel) = premier paiement ; ne peut depasser le total.
        BigDecimal total = saleEntity.getTotalAmount();
        BigDecimal down = request.downPayment() != null ? request.downPayment() : BigDecimal.ZERO;
        if (down.signum() > 0) {
            if (down.compareTo(total) > 0) {
                throw new BusinessException(
                        "L'acompte (" + down + ") ne peut depasser le total (" + total + ")",
                        HttpStatus.BAD_REQUEST);
            }
            credit.addPayment(CreditPayment.builder().amount(down).method(method.name()).build());
        }

        credit = creditRepository.save(credit);
        log.info("Credit id={} cree (vente #{}, client {}, total {}, acompte {})",
                credit.getId(), saleEntity.getId(), customer.getName(), total, down);
        return creditMapper.toResponse(credit, down);
    }

    private Customer resolveCustomer(CreditCreateRequest request) {
        if (request.customerId() != null) {
            return customerRepository.findById(request.customerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client", request.customerId()));
        }
        if (request.newCustomer() != null && request.newCustomer().name() != null
                && !request.newCustomer().name().isBlank()) {
            CustomerRequest nc = request.newCustomer();
            return customerRepository.save(Customer.builder()
                    .name(nc.name().trim()).phone(nc.phone()).address(nc.address()).build());
        }
        throw new BusinessException("Un client est obligatoire (customerId ou newCustomer)",
                HttpStatus.BAD_REQUEST);
    }

    // ------------------------------------- Paiement -------------------------------------

    @Transactional
    public CreditResponse addPayment(Long creditId, CreditPaymentRequest request) {
        Credit credit = getCredit(creditId);
        if (credit.isCancelled()) {
            throw new BusinessException("Credit annule : aucun paiement possible", HttpStatus.CONFLICT);
        }
        // amount > 0 garanti par la validation (@Positive).
        BigDecimal total = credit.getSale().getTotalAmount();
        BigDecimal paid = paymentRepository.sumPaidByCredit(creditId);
        BigDecimal remaining = total.subtract(paid);
        if (request.amount().compareTo(remaining) > 0) {
            throw new BusinessException(
                    "Paiement (" + request.amount() + ") superieur au reste du (" + remaining + ")",
                    HttpStatus.BAD_REQUEST);
        }
        credit.addPayment(CreditPayment.builder()
                .amount(request.amount())
                .method(request.method())
                .build());
        creditRepository.save(credit);
        BigDecimal newPaid = paid.add(request.amount());
        log.info("Paiement {} sur credit id={} (paye {} / {})", request.amount(), creditId, newPaid, total);
        return creditMapper.toResponse(credit, newPaid);
    }

    // ------------------------------------- Annulation -------------------------------------

    /**
     * Annulation TRACABLE : reintegre le stock de chaque ligne et cree un retour (reversal) qui
     * corrige le CA net via le mecanisme existant ; le credit est marque annule (exclu de la
     * tresorerie credits). Ni le stock ni le CA ne sont fausses.
     */
    @Transactional
    public CreditResponse cancel(Long creditId) {
        Credit credit = getCredit(creditId);
        if (credit.isCancelled()) {
            throw new BusinessException("Credit deja annule", HttpStatus.CONFLICT);
        }
        Sale sale = credit.getSale();
        for (SaleItem item : sale.getItems()) {
            Long variantId = item.getVariant().getId();
            int already = returnRepository.sumReturnedBySaleAndVariant(sale.getId(), variantId);
            int toReverse = item.getQuantity() - already;
            if (toReverse <= 0) continue;
            returnRepository.save(Return.builder()
                    .sale(sale).variant(item.getVariant()).quantity(toReverse)
                    .reason("Annulation credit #" + creditId).build());
            variantRepository.incrementStock(variantId, toReverse);   // restock (meme logique qu'un retour)
        }
        credit.setCancelled(true);
        creditRepository.save(credit);
        log.warn("[AUDIT] Credit id={} annule (vente #{} reversee, stock reintegre)", creditId, sale.getId());
        return creditMapper.toResponse(credit, paymentRepository.sumPaidByCredit(creditId));
    }

    // --------------------------------- Lecture / recherche ---------------------------------

    @Transactional(readOnly = true)
    public CreditResponse findById(Long id) {
        Credit credit = getCredit(id);
        return creditMapper.toResponse(credit, paymentRepository.sumPaidByCredit(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<CreditSummaryResponse> search(Long customerId, String status,
                                                      LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;
        String st = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        Page<CreditRow> page = creditRepository.search(customerId, st, start, end, pageable);
        return PageResponse.of(page, page.getContent().stream().map(creditMapper::toSummary).toList());
    }

    @Transactional(readOnly = true)
    public CreditDashboardResponse dashboard() {
        BigDecimal total = creditRepository.sumTotalActive();
        BigDecimal collected = creditRepository.sumCollectedActive();
        return new CreditDashboardResponse(
                creditRepository.countActive(), total, collected, total.subtract(collected));
    }

    private Credit getCredit(Long id) {
        return creditRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credit", id));
    }
}
