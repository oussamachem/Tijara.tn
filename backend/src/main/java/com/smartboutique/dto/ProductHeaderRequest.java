package com.smartboutique.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/** Mise a jour de l'entete d'un produit (les variantes sont gerees a part). */
public record ProductHeaderRequest(
        @NotBlank(message = "La reference est obligatoire")
        String reference,

        @NotBlank(message = "Le nom est obligatoire")
        String name,

        @Size(max = 1000, message = "La description ne peut depasser 1000 caracteres")
        String description,

        @NotNull(message = "La categorie est obligatoire")
        Long categoryId,

        @NotNull(message = "Le prix d'achat est obligatoire")
        @Positive(message = "Le prix d'achat doit etre positif")
        BigDecimal purchasePrice,

        @NotNull(message = "Le prix de vente est obligatoire")
        @Positive(message = "Le prix de vente doit etre positif")
        BigDecimal salePrice
) {
}
