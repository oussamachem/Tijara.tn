package com.smartboutique.entity;

import java.math.BigDecimal;

/** Statut d'une dette fournisseur, DERIVE (total saisi vs somme des paiements). */
public enum DebtStatus {
    UNPAID,    // aucun paiement
    PARTIAL,   // 0 < paye < total
    PAID;      // paye >= total

    public static DebtStatus of(BigDecimal total, BigDecimal paid) {
        if (paid.signum() <= 0) return UNPAID;
        if (paid.compareTo(total) >= 0) return PAID;
        return PARTIAL;
    }
}
