package com.smartboutique.dto;

import com.smartboutique.entity.DebtStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detail d'une dette fournisseur : reste et statut DERIVES (total saisi - paiements). */
public record DebtResponse(
        Long id,
        SupplierResponse supplier,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        DebtStatus status,
        LocalDate dueDate,
        String invoiceReference,
        String description,
        Long productId,
        String productName,
        LocalDateTime createdAt,
        List<DebtPaymentResponse> payments
) {
}
