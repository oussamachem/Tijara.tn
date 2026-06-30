package com.smartboutique.dto;

import java.math.BigDecimal;

/** Tableau de bord des dettes fournisseurs (comptes a payer). */
public record DebtDashboardResponse(
        long debtsCount,
        BigDecimal totalAmount,   // somme des totaux dus
        BigDecimal paid,          // deja paye
        BigDecimal outstanding    // restant a payer
) {
}
