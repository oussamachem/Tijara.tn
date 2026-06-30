package com.smartboutique.dto;

import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Modification d'une dette : fournisseur / echeance / reference / description librement.
 * Le {@code totalAmount} n'est modifiable QUE si aucun paiement n'a encore ete enregistre
 * (sinon le reste deviendrait incoherent) — applique cote service.
 */
public record DebtUpdateRequest(
        Long supplierId,

        @Positive(message = "Le montant total doit etre positif")
        BigDecimal totalAmount,

        LocalDate dueDate,

        String invoiceReference,

        String description
) {
}
