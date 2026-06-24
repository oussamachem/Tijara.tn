package com.smartboutique.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicateurs du tableau de bord.
 * Le chiffre d'affaires du jour est exprime NET : ventes du jour - valeur des retours du jour
 * (retours valorises au prix unitaire capture sur la vente d'origine).
 */
public record DashboardResponse(
        long totalProducts,
        long totalStock,
        long lowStockCount,
        long todaySalesCount,
        BigDecimal todayGrossRevenue,
        BigDecimal todayReturnsValue,
        BigDecimal todayNetRevenue,
        List<TopProduct> topSellingProducts
) {
}
