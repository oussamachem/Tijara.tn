package com.smartboutique.entity;

/**
 * Role CONTEXTUEL d'un utilisateur dans une boutique donnee.
 * OWNER  : proprietaire (gere tout : produits, stock, ventes, vendeurs, stats...).
 * VENDOR : vendeur (operations autorisees : ventes, scan, caisse, reservations...).
 */
public enum ShopMemberRole {
    OWNER,
    VENDOR
}
