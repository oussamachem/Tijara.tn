package com.smartboutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Creation / modification d'une taille du catalogue. */
public record SizeRequest(
        @NotBlank(message = "Le libelle est obligatoire")
        @Size(max = 50, message = "Le libelle ne peut depasser 50 caracteres")
        String label,

        @PositiveOrZero(message = "La position ne peut etre negative")
        Integer position
) {
}
