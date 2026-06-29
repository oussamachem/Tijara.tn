package com.smartboutique.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection SQL legere d'un credit pour la liste (paye calcule en base, pas de N+1).
 * Le reste et le statut sont derives ensuite cote service.
 */
public record CreditRow(
        Long id,
        Long customerId,
        String customerName,
        BigDecimal total,
        BigDecimal paid,
        LocalDate dueDate,
        boolean cancelled,
        LocalDateTime createdAt
) {
}
