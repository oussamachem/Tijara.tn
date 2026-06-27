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
        // Couverture = URL de l'image position 0 : lecture mono-image INCHANGEE (liste, mobile).
        String imageUrl,
        // Galerie complete triee par position (couverture en tete).
        List<ProductImageResponse> images,
        LocalDateTime createdAt,
        Integer totalStock,
        boolean lowStock,
        List<VariantResponse> variants
) {
}
