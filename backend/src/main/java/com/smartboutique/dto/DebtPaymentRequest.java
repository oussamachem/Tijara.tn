package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Paiement sur une dette fournisseur (montant strictement positif, <= reste). */
public record DebtPaymentRequest(
        @NotNull(message = "Le montant est obligatoire")
        @Positive(message = "Le montant doit etre strictement positif")
        BigDecimal amount,

        String method
) {
}
