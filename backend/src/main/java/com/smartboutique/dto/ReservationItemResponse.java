package com.smartboutique.dto;

import java.math.BigDecimal;

/** Ligne d'une reservation (attributs denormalises figes a la creation). */
public record ReservationItemResponse(
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
