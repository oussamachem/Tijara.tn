package com.smartboutique.dto;

import com.smartboutique.entity.Role;

import java.time.LocalDateTime;

/** Representation publique d'un utilisateur (jamais le mot de passe). */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {
}
