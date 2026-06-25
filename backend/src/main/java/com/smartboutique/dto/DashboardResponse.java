package com.smartboutique.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Indicateurs du tableau de bord. CA du jour exprime NET (ventes - valeur des retours du jour,
 * retours valorises au prix capture). Stock/rupture au grain VARIANTE.
 */
public record DashboardResponse(
        long totalProducts,
        long totalStock,
        long lowStockVariants,
        long lowStockProducts,
        long todaySalesCount,
        BigDecimal todayGrossRevenue,
        BigDecimal todayReturnsValue,
        BigDecimal todayNetRevenue,
        List<TopProduct> topSellingProducts
) {
}
