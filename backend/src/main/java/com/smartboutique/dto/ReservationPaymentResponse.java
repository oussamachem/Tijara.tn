package com.smartboutique.dto;

import com.smartboutique.entity.TenderMethod;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Un versement (journal) sur une reservation. */
public record ReservationPaymentResponse(
        Long id,
        BigDecimal amount,
        TenderMethod method,
        String sellerName,
        LocalDateTime createdAt
) {
}
