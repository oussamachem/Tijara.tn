package com.smartboutique.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Projection SQL legere d'une dette pour la liste (paye agrege en base, pas de N+1). */
public record DebtRow(
        Long id,
        Long supplierId,
        String supplierName,
        BigDecimal total,
        BigDecimal paid,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
}
