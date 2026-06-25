package com.smartboutique.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Representation d'un produit (modele) avec ses variantes et son stock total. */
public record ProductResponse(
        Long id,
        String reference,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        String imageUrl,
        LocalDateTime createdAt,
        Integer totalStock,
        boolean lowStock,
        List<VariantResponse> variants
) {
}
