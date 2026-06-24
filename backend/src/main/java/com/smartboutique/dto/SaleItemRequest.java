package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Une ligne demandee dans une vente. */
public record SaleItemRequest(
        @NotNull(message = "Le produit est obligatoire")
        Long productId,

        @NotNull(message = "La quantite est obligatoire")
        @Positive(message = "La quantite doit etre positive")
        Integer quantity
) {
}
