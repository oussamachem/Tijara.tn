package com.smartboutique.dto;

import java.math.BigDecimal;

/** Ligne d'une vente (attributs figes au moment de la vente). */
public record SaleItemResponse(
        Long id,
        Long variantId,
        String variantReference,
        String productName,
        String colorName,
        String size,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
