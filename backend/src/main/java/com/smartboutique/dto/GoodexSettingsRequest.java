package com.smartboutique.dto;

import jakarta.validation.constraints.Size;

/**
 * Réglages Goodex de la boutique. {@code token} vide = inchangé (on n'écrase pas un token existant
 * par mégarde). {@code baseUrl} vide = valeur par défaut appliquée côté service.
 */
public record GoodexSettingsRequest(
        @Size(max = 1000) String token,   // un JWT peut dépasser 255 caractères
        @Size(max = 60) String userId,
        @Size(max = 200) String baseUrl
) {
}
