package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;

/** Ajuste le stock d'un produit d'une valeur relative (positive = entree, negative = sortie). */
public record StockAdjustRequest(
        @NotNull(message = "La variation est obligatoire")
        Integer delta
) {
}
