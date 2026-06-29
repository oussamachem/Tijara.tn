package com.smartboutique.dto;

import java.math.BigDecimal;

/** Tableau de bord credits (tresorerie) : agregats sur les credits ACTIFS (non annules). */
public record CreditDashboardResponse(
        long creditsCount,
        BigDecimal totalAmount,    // somme des totaux vendus a credit
        BigDecimal collected,      // deja encaisse (somme des paiements)
        BigDecimal outstanding     // restant a encaisser (total - encaisse)
) {
}
