package com.smartboutique.dto;

import com.smartboutique.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Projection SQL pour les listes de reservations : le "paye" est deja agrege via sous-requete
 * (pas de N+1). Le reste et les jours restants sont derives dans le mapper.
 */
public record ReservationRow(
        Long id,
        String reference,
        String customerName,
        String customerPhone,
        ReservationStatus status,
        BigDecimal total,
        BigDecimal paid,
        LocalDateTime dueDate,
        boolean depositForfeited,
        LocalDateTime createdAt
) {
}
