package com.smartboutique.mapper;

import com.smartboutique.dto.*;
import com.smartboutique.entity.DebtPayment;
import com.smartboutique.entity.DebtStatus;
import com.smartboutique.entity.Supplier;
import com.smartboutique.entity.SupplierDebt;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** Conversion des dettes/fournisseurs vers leurs DTO. Reste et statut DERIVES. */
@Component
public class DebtMapper {

    public SupplierResponse toSupplierResponse(Supplier s) {
        return new SupplierResponse(s.getId(), s.getName(), s.getPhone(), s.getAddress());
    }

    public DebtPaymentResponse toPaymentResponse(DebtPayment p) {
        return new DebtPaymentResponse(p.getId(), p.getAmount(), p.getMethod(), p.getCreatedAt());
    }

    public DebtResponse toResponse(SupplierDebt debt, BigDecimal paid) {
        BigDecimal total = debt.getTotalAmount();
        List<DebtPaymentResponse> payments = debt.getPayments().stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .map(this::toPaymentResponse).toList();
        return new DebtResponse(
                debt.getId(),
                toSupplierResponse(debt.getSupplier()),
                total,
                paid,
                total.subtract(paid),
                DebtStatus.of(total, paid),
                debt.getDueDate(),
                debt.getInvoiceReference(),
                debt.getDescription(),
                debt.getProduct() != null ? debt.getProduct().getId() : null,
                debt.getProduct() != null ? debt.getProduct().getName() : null,
                debt.getCreatedAt(),
                payments);
    }

    public DebtSummaryResponse toSummary(DebtRow row) {
        return new DebtSummaryResponse(
                row.id(), row.supplierId(), row.supplierName(),
                row.total(), row.paid(), row.total().subtract(row.paid()),
                DebtStatus.of(row.total(), row.paid()),
                row.dueDate(), row.createdAt());
    }
}
