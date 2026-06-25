package com.smartboutique.dto;

import java.math.BigDecimal;

/**
 * Variante resolue (par scan QR ou liste stock) avec les infos produit necessaires
 * a la vente : nom, prix, couleur, taille, stock de la declinaison.
 */
public record VariantScanResponse(
        Long variantId,
        String variantReference,
        Long productId,
        String productName,
        BigDecimal salePrice,
        Long colorId,
        String colorName,
        String colorHex,
        String size,
        Integer quantity,
        Integer seuilAlerte,
        boolean lowStock
) {
}
