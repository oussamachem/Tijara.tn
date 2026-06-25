package com.smartboutique.dto;

/** Une declinaison (SKU) d'un produit. */
public record VariantResponse(
        Long id,
        Long colorId,
        String colorName,
        String colorHex,
        String size,
        Integer quantity,
        Integer seuilAlerte,
        boolean lowStock,
        String reference,
        String qrCode
) {
}
