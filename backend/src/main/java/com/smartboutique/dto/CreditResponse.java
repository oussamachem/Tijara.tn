package com.smartboutique.dto;

import com.smartboutique.entity.CreditStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Detail d'un credit : client, lignes (produits, prix captures), paiements, reste et statut derives. */
public record CreditResponse(
        Long id,
        Long saleId,
        CustomerResponse customer,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        CreditStatus status,
        LocalDate dueDate,
        boolean cancelled,
        LocalDateTime createdAt,
        List<SaleItemResponse> items,
        List<CreditPaymentResponse> payments
) {
}
