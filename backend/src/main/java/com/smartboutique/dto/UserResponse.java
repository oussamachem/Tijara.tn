package com.smartboutique.dto;

import java.time.LocalDateTime;

/**
 * Representation publique d'un utilisateur (jamais le mot de passe). Phase A : plus de role global
 * (le role est contextuel via shop_members). {@code platformAdmin} = admin plateforme.
 */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        boolean platformAdmin,
        boolean active,
        LocalDateTime createdAt
) {
}
