package com.smartboutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Creation / modification d'une couleur du catalogue. */
public record ColorRequest(
        @NotBlank(message = "Le nom est obligatoire")
        String name,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Code hexadecimal invalide (ex. #1E40AF)")
        String hex
) {
}
