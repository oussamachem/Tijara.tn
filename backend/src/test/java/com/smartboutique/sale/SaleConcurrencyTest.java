package com.smartboutique.sale;

import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.PaymentMethod;
import com.smartboutique.entity.Product;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ReturnRepository;
import com.smartboutique.repository.SaleRepository;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.smartboutique.support.AbstractPostgresIT;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de concurrence : des ventes simultanees sur le meme produit ne doivent jamais
 * faire passer le stock sous zero (anti-survente via decrement atomique conditionnel).
 */
class SaleConcurrencyTest extends AbstractPostgresIT {

    @Autowired
    private SaleService saleService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SaleRepository saleRepository;
    @Autowired
    private ReturnRepository returnRepository;

    private Long productId;
    private Long sellerId;

    private static final int INITIAL_STOCK = 10;
    private static final int THREADS = 20;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();

        Category category = categoryRepository.findByName("Homme")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Homme").build()));
        Product p = productRepository.save(Product.builder()
                .reference("REF-CONC").name("Produit concurrence").category(category)
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal("10.00"))
                .quantity(INITIAL_STOCK).seuilAlerte(0).qrCode("REF-CONC").build());
        this.productId = p.getId();
        this.sellerId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
    }

    @Test
    @DisplayName("20 ventes simultanees d'1 unite sur un stock de 10 : exactement 10 reussissent, stock final = 0")
    void concurrentSales_doNotOversell() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            pool.submit(() -> {
                try {
                    start.await(); // depart simultane
                    SaleRequest req = new SaleRequest(
                            List.of(new SaleItemRequest(productId, 1)), PaymentMethod.ESPECES, null);
                    saleService.createSale(req, sellerId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    insufficient.incrementAndGet(); // 409 stock insuffisant : attendu
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        int finalStock = productRepository.findById(productId).orElseThrow().getQuantity();

        // Invariant central : aucun survente.
        assertThat(finalStock).isGreaterThanOrEqualTo(0);
        assertThat(finalStock).isEqualTo(INITIAL_STOCK - success.get());
        assertThat(saleRepository.count()).isEqualTo(success.get());

        // Comportement attendu : tout le stock vendu, le reste refuse proprement.
        assertThat(unexpected.get()).isZero();
        assertThat(success.get()).isEqualTo(INITIAL_STOCK);
        assertThat(insufficient.get()).isEqualTo(THREADS - INITIAL_STOCK);
        assertThat(finalStock).isZero();
    }

    @Test
    @DisplayName("Dernier exemplaire vendu par 2 threads : un seul réussit, stock jamais négatif")
    void lastItem_twoThreads_onlyOneSucceeds() throws Exception {
        Product p = productRepository.save(Product.builder()
                .reference("LAST-ONE").name("Dernier").category(categoryRepository.findByName("Homme").orElseThrow())
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal("10.00"))
                .quantity(1).seuilAlerte(0).qrCode("LAST-ONE").build());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    saleService.createSale(
                            new SaleRequest(List.of(new SaleItemRequest(p.getId(), 1)), PaymentMethod.ESPECES, null),
                            sellerId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    insufficient.incrementAndGet();
                } catch (Exception e) {
                    unexpected.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(20, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        int finalStock = productRepository.findById(p.getId()).orElseThrow().getQuantity();
        assertThat(finalStock).isZero();           // jamais négatif
        assertThat(success.get()).isEqualTo(1);    // un seul gagne le dernier exemplaire
        assertThat(insufficient.get()).isEqualTo(1);
        assertThat(unexpected.get()).isZero();
    }
}
