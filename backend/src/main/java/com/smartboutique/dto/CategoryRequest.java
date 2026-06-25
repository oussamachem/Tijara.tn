package com.smartboutique.dto;

import com.smartboutique.entity.SizeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Creation / modification d'une categorie. */
public record CategoryRequest(
        @NotBlank(message = "Le nom est obligatoire")
        String name,

        @Size(max = 500, message = "La description ne peut depasser 500 caracteres")
        String description,

        @NotNull(message = "Le type de taille est obligatoire")
        SizeType sizeType
) {
}
