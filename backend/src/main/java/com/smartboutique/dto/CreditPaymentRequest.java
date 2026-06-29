package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Enregistrement d'un paiement sur un credit (montant strictement positif, <= reste). */
public record CreditPaymentRequest(
        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit etre strictement positif")
        BigDecimal amount,

        String method
) {
}
