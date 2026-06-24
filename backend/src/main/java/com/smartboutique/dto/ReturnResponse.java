package com.smartboutique.dto;

import java.time.LocalDateTime;

/** Retour enregistre. */
public record ReturnResponse(
        Long id,
        Long saleId,
        Long productId,
        String productReference,
        String productName,
        Integer quantity,
        String reason,
        LocalDateTime returnDate
) {
}
