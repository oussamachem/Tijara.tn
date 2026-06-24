package com.smartboutique.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Representation d'un produit. */
public record ProductResponse(
        Long id,
        String reference,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String size,
        String color,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        Integer quantity,
        Integer seuilAlerte,
        boolean lowStock,
        String imageUrl,
        String qrCode,
        LocalDateTime createdAt
) {
}
