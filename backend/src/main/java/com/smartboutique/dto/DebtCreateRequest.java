package com.smartboutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Creation d'une dette fournisseur. Le total est SAISI (facture). Aucun mouvement de stock.
 * Fournisseur existant ({@code supplierId}) OU nouveau ({@code newSupplier}). Produit optionnel
 * et purement descriptif.
 */
public record DebtCreateRequest(
        Long supplierId,

        @Valid
        SupplierRequest newSupplier,

        @NotNull(message = "Le montant total est obligatoire")
        @Positive(message = "Le montant total doit etre positif")
        BigDecimal totalAmount,

        LocalDate dueDate,

        String invoiceReference,

        String description,

        /** Reference produit DESCRIPTIVE (optionnelle, aucun effet stock). */
        Long productId,

        @PositiveOrZero(message = "L'acompte ne peut etre negatif")
        BigDecimal downPayment
) {
}
