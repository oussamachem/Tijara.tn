package com.smartboutique.entity;

/**
 * Cycle de vie d'une commande en ligne (C4) :
 * EN_ATTENTE -> CONFIRMEE (stock decremente, Phase 5) -> PRETE -> RECUPEREE ; ANNULEE (stock rendu).
 */
public enum OrderStatus {
    EN_ATTENTE,
    CONFIRMEE,
    PRETE,
    RECUPEREE,
    ANNULEE
}
