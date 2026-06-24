package com.smartboutique.service;

import com.smartboutique.dto.DashboardResponse;
import com.smartboutique.dto.TopProduct;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ReturnRepository;
import com.smartboutique.repository.SaleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Construit les indicateurs du tableau de bord. Toutes les agregations sont faites en base
 * (COUNT/SUM/GROUP BY) ; aucun chargement complet en memoire.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final ReturnRepository returnRepository;

    /** Nombre de produits dans le palmares des meilleures ventes. */
    @Value("${app.dashboard.top-products:5}")
    private int topProductsLimit;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long totalProducts = productRepository.count();
        long totalStock = productRepository.sumAllQuantities();
        long lowStockCount = productRepository.countLowStock();

        long todaySalesCount = saleRepository.countBySaleDateBetween(start, end);
        BigDecimal grossRevenue = saleRepository.sumTotalAmountBetween(start, end);
        BigDecimal returnsValue = returnRepository.sumReturnValueBetween(start, end);
        // CA net = ventes du jour - valeur des retours du jour.
        BigDecimal netRevenue = grossRevenue.subtract(returnsValue);

        List<TopProduct> topProducts =
                saleRepository.findTopSellingProducts(PageRequest.of(0, topProductsLimit));

        return new DashboardResponse(
                totalProducts, totalStock, lowStockCount,
                todaySalesCount, grossRevenue, returnsValue, netRevenue,
                topProducts);
    }
}
