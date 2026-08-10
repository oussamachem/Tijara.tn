package com.smartboutique.dto;

/** Réglages Goodex renvoyés au propriétaire. Le token n'est JAMAIS exposé en clair ({@code configured}). */
public record GoodexSettingsResponse(
        String userId,
        String baseUrl,
        boolean configured
) {
}
