package com.smartboutique.entity;

import java.math.BigDecimal;

/** Statut de paiement d'un credit, DERIVE (total vs somme des paiements). */
public enum CreditStatus {
    UNPAID,    // aucun paiement
    PARTIAL,   // 0 < paye < total
    PAID,      // paye >= total
    CANCELLED; // credit annule (reversal)

    /** Derive le statut a partir du total, du paye et de l'etat d'annulation. */
    public static CreditStatus of(BigDecimal total, BigDecimal paid, boolean cancelled) {
        if (cancelled) return CANCELLED;
        if (paid.signum() <= 0) return UNPAID;
        if (paid.compareTo(total) >= 0) return PAID;
        return PARTIAL;
    }
}
