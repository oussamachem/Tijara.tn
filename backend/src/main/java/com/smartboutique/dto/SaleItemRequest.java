package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Une ligne demandee dans une vente : cible une VARIANTE. */
public record SaleItemRequest(
        @NotNull(message = "La variante est obligatoire")
        Long variantId,

        @NotNull(message = "La quantite est obligatoire")
        @Positive(message = "La quantite doit etre positive")
        Integer quantity
) {
}
