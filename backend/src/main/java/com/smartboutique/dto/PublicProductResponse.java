package com.smartboutique.dto;

import java.math.BigDecimal;
import java.util.List;

/** Produit du catalogue public d'une boutique (declinaisons disponibles + galerie photos). */
public record PublicProductResponse(
        Long productId,
        String reference,
        String name,
        BigDecimal price,
        List<PublicVariantResponse> variants,
        List<PublicImage> images
) {
    /** Une declinaison disponible (stock > 0) proposee au client. */
    public record PublicVariantResponse(Long variantId, String color, String size, Integer available) {
    }

    /** Une photo publique du produit (chemin relatif sous /uploads, ordre par position ; 0 = couverture). */
    public record PublicImage(String url, int position) {
    }
}
