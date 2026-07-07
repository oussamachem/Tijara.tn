package com.smartboutique.entity;

/**
 * Roles applicatifs.
 * SUPER_ADMIN : plateforme (multi-boutiques) — gere les boutiques, hors tenant.
 * ADMIN       : administrateur d'UNE boutique (back-office web), scope a son tenant.
 * VENDEUR     : effectue les ventes d'une boutique (mobile), scope a son tenant.
 * CLIENT      : compte GLOBAL du marketplace (commande chez plusieurs boutiques), sans tenant fixe.
 */
public enum Role {
    SUPER_ADMIN,
    ADMIN,
    VENDEUR,
    CLIENT
}
