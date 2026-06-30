package com.smartboutique.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un paiement enregistre sur une dette fournisseur. */
public record DebtPaymentResponse(
        Long id,
        BigDecimal amount,
        String method,
        LocalDateTime createdAt
) {
}
