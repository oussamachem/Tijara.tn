package com.smartboutique.dto;

import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;

import java.time.LocalDateTime;

/** Vue d'une boutique (tenant) pour l'espace SUPER_ADMIN. */
public record BoutiqueResponse(
        Long id,
        String name,
        String slug,
        BoutiqueStatus status,
        LocalDateTime createdAt
) {
    public static BoutiqueResponse of(Boutique b) {
        return new BoutiqueResponse(b.getId(), b.getName(), b.getSlug(), b.getStatus(), b.getCreatedAt());
    }
}
