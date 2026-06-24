package com.smartboutique.dto;

import java.math.BigDecimal;

/** Ligne d'une vente (prix figes au moment de la vente). */
public record SaleItemResponse(
        Long id,
        Long productId,
        String productReference,
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
