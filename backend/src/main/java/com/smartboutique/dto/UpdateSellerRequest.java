package com.smartboutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Modification d'un compte vendeur (ADMIN). Le mot de passe n'est pas modifie ici. */
public record UpdateSellerRequest(
        @NotBlank(message = "Le nom complet est obligatoire")
        String fullName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email
) {
}
