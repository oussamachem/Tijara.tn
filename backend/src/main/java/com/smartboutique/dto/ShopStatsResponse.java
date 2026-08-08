package com.smartboutique.dto;

/**
 * Statistiques publiques d'une boutique (profil « façon TikTok ») : abonnés, ventes réalisées,
 * produits. Agrégats seulement — aucune donnée sensible.
 */
public record ShopStatsResponse(long followers, long sales, long products) {
}
