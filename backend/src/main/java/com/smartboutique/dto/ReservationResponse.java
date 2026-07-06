package com.smartboutique.dto;

import com.smartboutique.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detail complet d'une reservation : lignes + versements, reste et jours restants DERIVES
 * (serveur). {@code saleId} = vente de reconnaissance du CA (renseignee a la cloture).
 */
public record ReservationResponse(
        Long id,
        String reference,
        String customerName,
        String customerPhone,
        String sellerName,
        ReservationStatus status,
        BigDecimal total,
        BigDecimal paid,
        BigDecimal remaining,
        long daysRemaining,
        LocalDateTime createdAt,
        LocalDateTime dueDate,
        LocalDateTime closedAt,
        boolean depositForfeited,
        Long saleId,
        List<ReservationItemResponse> items,
        List<ReservationPaymentResponse> payments
) {
}
