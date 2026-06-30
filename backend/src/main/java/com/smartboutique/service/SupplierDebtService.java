package com.smartboutique.service;

import com.smartboutique.dto.*;
import com.smartboutique.entity.DebtPayment;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.Supplier;
import com.smartboutique.entity.SupplierDebt;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.DebtMapper;
import com.smartboutique.repository.DebtPaymentRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.SupplierDebtRepository;
import com.smartboutique.repository.SupplierRepository;
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
 * Dettes fournisseurs (comptes a payer). Registre PUREMENT FINANCIER : aucun mouvement de stock,
 * aucun impact CA. Total SAISI ; reste/statut DERIVES (BigDecimal).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierDebtService {

    private final SupplierDebtRepository debtRepository;
    private final DebtPaymentRepository paymentRepository;
    private final SupplierRepository supplierRepository;
    private final ProductRepository productRepository;
    private final DebtMapper debtMapper;

    @Transactional
    public DebtResponse create(DebtCreateRequest request) {
        Supplier supplier = resolveSupplier(request);
        // Lien produit DESCRIPTIF (optionnel) : aucune lecture/ecriture de stock.
        Product product = request.productId() == null ? null
                : productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produit", request.productId()));

        SupplierDebt debt = SupplierDebt.builder()
                .supplier(supplier)
                .totalAmount(request.totalAmount())     // SAISI
                .dueDate(request.dueDate())
                .invoiceReference(request.invoiceReference())
                .description(request.description())
                .product(product)
                .build();

        BigDecimal down = request.downPayment() != null ? request.downPayment() : BigDecimal.ZERO;
        if (down.signum() > 0) {
            if (down.compareTo(request.totalAmount()) > 0) {
                throw new BusinessException(
                        "L'acompte (" + down + ") ne peut depasser le total (" + request.totalAmount() + ")",
                        HttpStatus.BAD_REQUEST);
            }
            debt.addPayment(DebtPayment.builder().amount(down).method("ESPECES").build());
        }

        debt = debtRepository.save(debt);
        log.info("Dette id={} creee (fournisseur {}, total {}, acompte {})",
                debt.getId(), supplier.getName(), request.totalAmount(), down);
        return debtMapper.toResponse(debt, down);
    }

    private Supplier resolveSupplier(DebtCreateRequest request) {
        if (request.supplierId() != null) {
            return supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", request.supplierId()));
        }
        if (request.newSupplier() != null && request.newSupplier().name() != null
                && !request.newSupplier().name().isBlank()) {
            SupplierRequest ns = request.newSupplier();
            return supplierRepository.save(Supplier.builder()
                    .name(ns.name().trim()).phone(ns.phone()).address(ns.address()).build());
        }
        throw new BusinessException("Un fournisseur est obligatoire (supplierId ou newSupplier)",
                HttpStatus.BAD_REQUEST);
    }

    @Transactional
    public DebtResponse addPayment(Long debtId, DebtPaymentRequest request) {
        SupplierDebt debt = getDebt(debtId);
        BigDecimal total = debt.getTotalAmount();
        BigDecimal paid = paymentRepository.sumPaidByDebt(debtId);
        BigDecimal remaining = total.subtract(paid);
        if (request.amount().compareTo(remaining) > 0) {
            throw new BusinessException(
                    "Paiement (" + request.amount() + ") superieur au reste du (" + remaining + ")",
                    HttpStatus.BAD_REQUEST);
        }
        debt.addPayment(DebtPayment.builder().amount(request.amount()).method(request.method()).build());
        debtRepository.save(debt);
        return debtMapper.toResponse(debt, paid.add(request.amount()));
    }

    @Transactional
    public DebtResponse update(Long id, DebtUpdateRequest request) {
        SupplierDebt debt = getDebt(id);
        if (request.supplierId() != null) {
            debt.setSupplier(supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", request.supplierId())));
        }
        debt.setDueDate(request.dueDate());
        debt.setInvoiceReference(request.invoiceReference());
        debt.setDescription(request.description());
        // Le total n'est modifiable que tant qu'aucun paiement n'a ete enregistre.
        if (request.totalAmount() != null && request.totalAmount().compareTo(debt.getTotalAmount()) != 0) {
            if (paymentRepository.existsByDebtId(id)) {
                throw new BusinessException(
                        "Le montant total n'est plus modifiable : des paiements ont ete enregistres",
                        HttpStatus.CONFLICT);
            }
            debt.setTotalAmount(request.totalAmount());
        }
        debtRepository.save(debt);
        return debtMapper.toResponse(debt, paymentRepository.sumPaidByDebt(id));
    }

    /** Suppression bloquee si des paiements existent (ne pas perdre l'historique des sorties). */
    @Transactional
    public void delete(Long id) {
        SupplierDebt debt = getDebt(id);
        if (paymentRepository.existsByDebtId(id)) {
            throw new BusinessException(
                    "Suppression impossible : des paiements sont enregistres sur cette dette",
                    HttpStatus.CONFLICT);
        }
        debtRepository.delete(debt);
        log.info("Dette id={} supprimee (sans paiement)", id);
    }

    @Transactional(readOnly = true)
    public DebtResponse findById(Long id) {
        return debtMapper.toResponse(getDebt(id), paymentRepository.sumPaidByDebt(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DebtSummaryResponse> search(Long supplierId, String status,
                                                    LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;
        String st = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;
        Page<DebtRow> page = debtRepository.search(supplierId, st, start, end, pageable);
        return PageResponse.of(page, page.getContent().stream().map(debtMapper::toSummary).toList());
    }

    @Transactional(readOnly = true)
    public DebtDashboardResponse dashboard() {
        BigDecimal total = debtRepository.sumTotal();
        BigDecimal paid = debtRepository.sumPaid();
        return new DebtDashboardResponse(debtRepository.countAll(), total, paid, total.subtract(paid));
    }

    private SupplierDebt getDebt(Long id) {
        return debtRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dette", id));
    }
}
