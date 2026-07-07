package com.smartboutique.notification;

import com.smartboutique.entity.OrderStatus;

/**
 * Evenement publie a chaque changement de statut d'une commande. Sert de point d'extension pour
 * les notifications client (email / push / SMS) : aujourd'hui un simple log (voir
 * {@link OrderNotificationListener}), demain un envoi reel sans toucher au service metier.
 */
public record OrderStatusChangedEvent(
        Long orderId,
        String reference,
        Long clientId,
        String clientEmail,
        OrderStatus from,
        OrderStatus to
) {
}
