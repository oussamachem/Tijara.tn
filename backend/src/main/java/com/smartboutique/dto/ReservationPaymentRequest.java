package com.smartboutique.dto;

import com.smartboutique.entity.TenderMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Un versement sur une reservation : montant strictement positif + moyen de reglement. */
public record ReservationPaymentRequest(
        @NotNull @Positive BigDecimal amount,
        @NotNull TenderMethod method
) {
}
