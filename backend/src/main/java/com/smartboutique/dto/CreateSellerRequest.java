package com.smartboutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Creation d'un compte vendeur (ADMIN). */
public record CreateSellerRequest(
        @NotBlank(message = "Le nom complet est obligatoire")
        String fullName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caracteres")
        String password
) {
}
