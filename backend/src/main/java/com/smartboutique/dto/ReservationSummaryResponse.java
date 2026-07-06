package com.smartboutique.dto;

import com.smartboutique.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ligne de liste d'une reservation : reste et jours restants DERIVES. {@code dueSoon} = true si
 * ACTIVE et jours restants <= seuil d'alerte (B5) -> badge "prevenir le client".
 */
public record ReservationSummaryResponse(
        Long id,
        String reference,
        String customerName,
        String customerPhone,
        ReservationStatus status,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        long daysRemaining,
        boolean dueSoon,
        boolean depositForfeited,
        LocalDateTime dueDate,
        LocalDateTime createdAt
) {
}
