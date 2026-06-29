package com.smartboutique.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un paiement enregistre sur un credit. */
public record CreditPaymentResponse(
        Long id,
        BigDecimal amount,
        String method,
        LocalDateTime createdAt
) {
}
