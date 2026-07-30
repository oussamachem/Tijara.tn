package com.smartboutique.dto;

import com.smartboutique.entity.Notification;

import java.time.LocalDateTime;

/** Notification in-app exposee au client. */
public record NotificationResponse(
        Long id,
        String type,
        String title,
        String body,
        Long orderId,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationResponse of(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getOrderId(), n.isRead(), n.getCreatedAt());
    }
}
