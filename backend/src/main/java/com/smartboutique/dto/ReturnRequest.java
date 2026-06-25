package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** Demande d'enregistrement d'un retour : cible une VARIANTE d'une vente. */
public record ReturnRequest(
        @NotNull(message = "La vente d'origine est obligatoire")
        Long saleId,

        @NotNull(message = "La variante est obligatoire")
        Long variantId,

        @NotNull(message = "La quantite est obligatoire")
        @Positive(message = "La quantite doit etre positive")
        Integer quantity,

        @Size(max = 500, message = "Le motif ne peut depasser 500 caracteres")
        String reason
) {
}
