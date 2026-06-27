package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/** Ajout d'une variante a un produit existant. */
public record AddVariantRequest(
        @NotNull(message = "La couleur est obligatoire")
        Long colorId,

        @NotNull(message = "La taille est obligatoire")
        Long sizeId,

        @NotNull(message = "La quantite est obligatoire")
        @PositiveOrZero(message = "La quantite ne peut etre negative")
        Integer quantity,

        @PositiveOrZero(message = "Le seuil ne peut etre negatif")
        Integer seuilAlerte
) {
}
