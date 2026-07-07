package com.smartboutique.dto;

import com.smartboutique.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Ligne de liste des commandes cote boutique-admin. */
public record OrderAdminResponse(
        Long id,
        String reference,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        String clientName,
        String clientEmail,
        int itemCount
) {
}
