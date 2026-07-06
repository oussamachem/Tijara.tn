package com.smartboutique.entity;

/**
 * Cycle de vie d'une reservation (layaway).
 *
 * <pre>
 *   ACTIVE ----(solde)----> COMPLETED   (CA reconnu, produit remis)
 *   ACTIVE ----(echeance)-> EXPIRED     (stock rendu, acompte retenu B6)
 *   ACTIVE ----(annulee)--> CANCELLED   (stock rendu, acompte retenu B6)
 * </pre>
 */
public enum ReservationStatus {
    ACTIVE,
    COMPLETED,
    EXPIRED,
    CANCELLED
}
