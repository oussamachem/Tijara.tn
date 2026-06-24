package com.smartboutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Mise a jour du profil par l'utilisateur connecte. */
public record UpdateProfileRequest(
        @NotBlank(message = "Le nom complet est obligatoire")
        String fullName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email
) {
}
