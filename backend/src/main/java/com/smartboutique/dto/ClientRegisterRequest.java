package com.smartboutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Inscription d'un compte CLIENT (global, marketplace) + coordonnées de livraison (optionnelles). */
public record ClientRegisterRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6) String password,
        @Size(max = 30) String phone,
        @Size(max = 300) String address,
        @Size(max = 40) String governorat
) {
}
