package com.smartboutique.dto;

import com.smartboutique.entity.DebtStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ligne de la liste des dettes (reste et statut derives cote serveur). */
public record DebtSummaryResponse(
        Long id,
        Long supplierId,
        String supplierName,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        DebtStatus status,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
}
