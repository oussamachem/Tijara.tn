package com.smartboutique.dto;

import com.smartboutique.entity.CreditStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ligne de la liste des credits (reste et statut derives cote serveur). */
public record CreditSummaryResponse(
        Long id,
        Long customerId,
        String customerName,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        CreditStatus status,
        LocalDate dueDate,
        boolean cancelled,
        LocalDateTime createdAt
) {
}
