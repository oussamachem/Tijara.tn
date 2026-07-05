package com.smartboutique.dashboard;

import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.entity.*;
import com.smartboutique.repository.*;
import com.smartboutique.service.SaleService;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Frontiere de journee metier (Africa/Tunis) sur PostgreSQL reel, au grain variante.
 * Deux ventes autour de minuit (23h30 et 00h30) tombent dans des jours distincts.
 */
class DayBoundaryTest extends AbstractPostgresIT {

    @Autowired private SaleService saleService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long adminId;
    private Long variantId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        adminId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color color = Fixtures.color(colorRepository, "Bleu");
        variantId = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, color,
                "BORDER", "M", 100, 0, "10.00").getId();
    }

    private Long sell() {
        return saleService.createSale(
                new SaleRequest(List.of(new SaleItemRequest(variantId, 1)), PaymentMethod.ESPECES, null, null),
                adminId).id();
    }

    private void forceSaleDate(Long saleId, LocalDateTime when) {
        jdbcTemplate.update("UPDATE sales SET sale_date = ? WHERE id = ?", Timestamp.valueOf(when), saleId);
    }

    @Test
    @DisplayName("Ventes a 23h30 et 00h30 (Tunis) tombent dans des jours metier distincts")
    void salesAroundMidnight_bucketedByBusinessDay() {
        Long lateSale = sell();
        Long earlySale = sell();
        forceSaleDate(lateSale, LocalDateTime.of(2026, 6, 22, 23, 30));
        forceSaleDate(earlySale, LocalDateTime.of(2026, 6, 23, 0, 30));

        var day22 = saleService.searchHistory(
                LocalDate.of(2026, 6, 22), LocalDate.of(2026, 6, 22), adminId, PageRequest.of(0, 10));
        var day23 = saleService.searchHistory(
                LocalDate.of(2026, 6, 23), LocalDate.of(2026, 6, 23), adminId, PageRequest.of(0, 10));

        assertThat(day22.totalElements()).isEqualTo(1);
        assertThat(day22.content()).extracting(SaleSummaryResponse::id).containsExactly(lateSale);
        assertThat(day23.totalElements()).isEqualTo(1);
        assertThat(day23.content()).extracting(SaleSummaryResponse::id).containsExactly(earlySale);
    }
}
