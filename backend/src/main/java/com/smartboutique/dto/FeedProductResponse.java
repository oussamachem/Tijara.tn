package com.smartboutique.dto;

import java.math.BigDecimal;

/**
 * Une carte du fil marketplace (accueil client) : un produit + la boutique qui le vend. Champs
 * strictement PUBLICS (déjà exposés par le catalogue) -> pas de fuite. La navigation cible
 * /s/{shopSlug}/p/{productId}.
 */
public record FeedProductResponse(
        String shopSlug,
        String shopName,
        Long productId,
        String name,
        BigDecimal price,
        String imageUrl
) {
}
