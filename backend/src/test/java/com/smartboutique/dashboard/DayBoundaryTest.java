package com.smartboutique.dashboard;

import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.PaymentMethod;
import com.smartboutique.entity.Product;
import com.smartboutique.repository.*;
import com.smartboutique.service.SaleService;
import com.smartboutique.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Frontière de journée métier (Africa/Tunis) sur PostgreSQL réel.
 * Deux ventes autour de minuit (23h30 et 00h30) doivent tomber dans des jours distincts.
 * C'est le cas limite que H2 n'a jamais couvert.
 */
class DayBoundaryTest extends AbstractPostgresIT {

    @Autowired private SaleService saleService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long adminId;
    private Long productId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        adminId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
        Category cat = categoryRepository.findByName("Homme")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Homme").build()));
        productId = productRepository.save(Product.builder()
                .reference("BORDER").name("Border").category(cat)
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal("10.00"))
                .quantity(100).seuilAlerte(0).qrCode("BORDER").build()).getId();
    }

    private Long sell() {
        return saleService.createSale(
                new SaleRequest(List.of(new SaleItemRequest(productId, 1)), PaymentMethod.ESPECES, null),
                adminId).id();
    }

    private void forceSaleDate(Long saleId, LocalDateTime when) {
        jdbcTemplate.update("UPDATE sales SET sale_date = ? WHERE id = ?", Timestamp.valueOf(when), saleId);
    }

    @Test
    @DisplayName("Ventes à 23h30 et 00h30 (Tunis) tombent dans des jours métier distincts")
    void salesAroundMidnight_bucketedByBusinessDay() {
        Long lateSale = sell();   // jour J à 23h30
        Long earlySale = sell();  // jour J+1 à 00h30
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
