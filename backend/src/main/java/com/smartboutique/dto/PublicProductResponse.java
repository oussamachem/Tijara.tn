package com.smartboutique.dto;

import java.math.BigDecimal;
import java.util.List;

/** Produit du catalogue public d'une boutique (avec ses declinaisons disponibles). */
public record PublicProductResponse(
        Long productId,
        String reference,
        String name,
        BigDecimal price,
        List<PublicVariantResponse> variants
) {
    /** Une declinaison disponible (stock > 0) proposee au client. */
    public record PublicVariantResponse(Long variantId, String color, String size, Integer available) {
    }
}
