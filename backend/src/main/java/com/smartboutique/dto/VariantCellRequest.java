package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Une cellule de la matrice couleur x taille a la creation d'un produit
 * (= une variante a creer avec son stock).
 */
public record VariantCellRequest(
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
