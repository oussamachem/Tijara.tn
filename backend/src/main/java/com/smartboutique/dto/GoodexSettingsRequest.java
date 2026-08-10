package com.smartboutique.dto;

import jakarta.validation.constraints.Size;

/**
 * Réglages Goodex de la boutique. {@code token} vide = inchangé (on n'écrase pas un token existant
 * par mégarde). {@code baseUrl} vide = valeur par défaut appliquée côté service.
 */
public record GoodexSettingsRequest(
        @Size(max = 255) String token,
        @Size(max = 60) String userId,
        @Size(max = 200) String baseUrl
) {
}
