package com.smartboutique.sale;

import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.entity.*;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.repository.*;
import com.smartboutique.service.SaleService;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrence au grain VARIANTE : des ventes simultanees sur la meme variante ne doivent
 * jamais faire passer son stock sous zero (decrement atomique conditionnel).
 */
class SaleConcurrencyTest extends AbstractPostgresIT {

    @Autowired private SaleService saleService;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;

    private Long variantId;
    private Long sellerId;

    private static final int INITIAL_STOCK = 10;
    private static final int THREADS = 20;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();

        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color color = Fixtures.color(colorRepository, "Bleu");
        variantId = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, color,
                "REF-CONC", "M", INITIAL_STOCK, 0, "10.00").getId();
        sellerId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
    }

    private void sellOneConcurrently(Long vId, int threads, int initialStock) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger insufficient = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    saleService.createSale(new SaleRequest(
                            List.of(new SaleItemRequest(vId, 1)), PaymentMethod.ESPECES, null), sellerId);
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
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        int finalStock = variantRepository.findById(vId).orElseThrow().getQuantity();
        assertThat(finalStock).isGreaterThanOrEqualTo(0);
        assertThat(finalStock).isEqualTo(initialStock - success.get());
        assertThat(unexpected.get()).isZero();
        assertThat(success.get()).isEqualTo(initialStock);
        assertThat(insufficient.get()).isEqualTo(threads - initialStock);
        assertThat(finalStock).isZero();
    }

    @Test
    @DisplayName("20 ventes simultanees / stock variante 10 : exactement 10 reussissent, stock 0")
    void concurrentSales_doNotOversell() throws Exception {
        sellOneConcurrently(variantId, THREADS, INITIAL_STOCK);
    }

    @Test
    @DisplayName("Dernier exemplaire d'une variante / 2 threads : un seul reussit, stock jamais negatif")
    void lastItem_twoThreads_onlyOneSucceeds() throws Exception {
        Category cat = categoryRepository.findByName("Homme").orElseThrow();
        Color color = Fixtures.color(colorRepository, "Rouge");
        Long lastVariant = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, color,
                "REF-LAST", "L", 1, 0, "10.00").getId();
        sellOneConcurrently(lastVariant, 2, 1);
    }
}
