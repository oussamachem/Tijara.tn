package com.smartboutique.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Creation d'une boutique (tenant) + son BOUTIQUE_ADMIN initial, par le SUPER_ADMIN.
 * {@code slug} optionnel : derive du nom si absent.
 */
public record CreateBoutiqueRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 140) String slug,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 6) String adminPassword,
        String adminName
) {
}
