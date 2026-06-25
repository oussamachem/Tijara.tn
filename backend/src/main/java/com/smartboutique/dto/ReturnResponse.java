package com.smartboutique.dto;

import java.time.LocalDateTime;

/** Retour enregistre (au grain variante). */
public record ReturnResponse(
        Long id,
        Long saleId,
        Long variantId,
        String variantReference,
        String productName,
        String colorName,
        String size,
        Integer quantity,
        String reason,
        LocalDateTime returnDate
) {
}
