package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Definit la quantite absolue en stock d'un produit. */
public record StockSetRequest(
        @NotNull(message = "La quantite est obligatoire")
        @PositiveOrZero(message = "La quantite ne peut etre negative")
        Integer quantity
) {
}
