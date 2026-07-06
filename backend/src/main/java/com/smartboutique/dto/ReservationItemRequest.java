package com.smartboutique.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Ligne d'une reservation a creer : une variante + une quantite. Prix calcule serveur. */
public record ReservationItemRequest(
        @NotNull Long variantId,
        @NotNull @Positive Integer quantity
) {
}
