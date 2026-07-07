package com.smartboutique.dto;

import com.smartboutique.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Detail d'une commande en ligne (cote client ou boutique-admin). */
public record OrderResponse(
        Long id,
        String reference,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        List<Item> items
) {
    public record Item(String productName, String color, String size, Integer quantity, BigDecimal unitPrice) {
    }
}
