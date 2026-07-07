package com.smartboutique.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

/** Passage de commande : lignes (variante + quantite). Le total est calcule serveur. */
public record OrderCreateRequest(
        @NotEmpty @Valid List<Line> items
) {
    public record Line(@NotNull Long variantId, @NotNull @Positive Integer quantity) {
    }
}
