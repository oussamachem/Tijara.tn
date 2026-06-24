package com.smartboutique.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Creation / modification d'un produit.
 * Prix strictement positifs ; quantite et seuil >= 0 (un produit peut etre cree en rupture).
 */
public record ProductRequest(
        @NotBlank(message = "La reference est obligatoire")
        String reference,

        @NotBlank(message = "Le nom est obligatoire")
        String name,

        @Size(max = 1000, message = "La description ne peut depasser 1000 caracteres")
        String description,

        Long categoryId,

        String size,

        String color,

        @NotNull(message = "Le prix d'achat est obligatoire")
        @Positive(message = "Le prix d'achat doit etre positif")
        BigDecimal purchasePrice,

        @NotNull(message = "Le prix de vente est obligatoire")
        @Positive(message = "Le prix de vente doit etre positif")
        BigDecimal salePrice,

        @NotNull(message = "La quantite est obligatoire")
        @PositiveOrZero(message = "La quantite ne peut etre negative")
        Integer quantity,

        @NotNull(message = "Le seuil d'alerte est obligatoire")
        @PositiveOrZero(message = "Le seuil d'alerte ne peut etre negatif")
        Integer seuilAlerte
) {
}
