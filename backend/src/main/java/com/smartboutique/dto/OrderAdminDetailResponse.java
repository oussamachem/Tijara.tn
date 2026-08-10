package com.smartboutique.dto;

import com.smartboutique.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Detail complet d'une commande cote boutique-admin : client, lignes, historique des statuts. */
public record OrderAdminDetailResponse(
        Long id,
        String reference,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        String clientName,
        String clientEmail,
        // Livraison (snapshot) + suivi transporteur Goodex
        String deliveryName,
        String deliveryPhone,
        String deliveryAddress,
        String deliveryGovernorat,
        String carrierEan,
        String carrierStatus,
        LocalDateTime carrierStatusAt,
        List<OrderResponse.Item> items,
        List<StatusEvent> history
) {
    public record StatusEvent(OrderStatus fromStatus, OrderStatus toStatus, String changedBy, LocalDateTime at) {
    }
}
