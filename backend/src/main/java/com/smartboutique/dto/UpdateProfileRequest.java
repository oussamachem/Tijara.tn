package com.smartboutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mise a jour du profil par l'utilisateur connecte (+ coordonnées de livraison optionnelles). */
public record UpdateProfileRequest(
        @NotBlank(message = "Le nom complet est obligatoire")
        String fullName,

        @NotBlank(message = "L'email est obligatoire")
        @Email(message = "Email invalide")
        String email,

        @Size(max = 30) String phone,
        @Size(max = 300) String address,
        @Size(max = 40) String governorat
) {
}
