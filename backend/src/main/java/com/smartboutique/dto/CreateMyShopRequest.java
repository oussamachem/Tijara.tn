package com.smartboutique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Self-service (Phase B) : un utilisateur AUTHENTIFIE cree SA boutique et en devient OWNER.
 * Seul le nom est requis ; le slug est derive et rendu unique cote serveur.
 */
public record CreateMyShopRequest(
        @NotBlank(message = "Le nom de la boutique est obligatoire")
        @Size(max = 120, message = "Le nom ne peut depasser 120 caracteres")
        String name
) {
}
