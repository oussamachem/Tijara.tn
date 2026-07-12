package com.smartboutique.reservation;
import com.smartboutique.support.WithShopMember;

import com.smartboutique.dto.ReservationItemRequest;
import com.smartboutique.dto.ReservationPaymentRequest;
import com.smartboutique.dto.ReservationCreateRequest;
import com.smartboutique.dto.ReservationResponse;
import com.smartboutique.entity.*;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.repository.*;
import com.smartboutique.service.ReservationService;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Feature B — Reservation (layaway) : stock retenu a la creation, CA reconnu a la cloture,
 * expiration/annulation avec stock rendu + acompte retenu (B6), concurrence au grain variante.
 */
@AutoConfigureMockMvc
@WithShopMember
class ReservationIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private ReservationPaymentRepository reservationPaymentRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationService reservationService;

    private Long v100;   // prix 100, stock 5
    private Long adminId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        reservationRepository.deleteAll();   // cascade -> reservation_items / reservation_payments
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color bleu = Fixtures.color(colorRepository, "Bleu");
        v100 = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, bleu, "R100", "M", 5, 0, "100.00").getId();
        adminId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
    }

    /**
     * La suite partage un meme PostgreSQL (Testcontainers singleton ou DB externe). On nettoie les
     * reservations apres chaque test pour ne pas laisser de lignes qui referencent product_variant
     * / sales et casseraient les deleteAll() des autres classes de test.
     */
    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post("/api/reservations").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int stock(Long id) { return variantRepository.findById(id).orElseThrow().getQuantity(); }

    private String createBody(int qty, String down, Integer durationDays) {
        String dur = durationDays == null ? "" : ",\"durationDays\":" + durationDays;
        return "{\"customerName\":\"Ali\",\"customerPhone\":\"20123456\",\"items\":["
                + "{\"variantId\":" + v100 + ",\"quantity\":" + qty + "}],"
                + "\"downPayment\":" + down + ",\"downPaymentMethod\":\"ESPECES\"" + dur + "}";
    }

    @Test
    @DisplayName("Creer : stock retenu (5->4), acompte enregistre, reste correct, ACTIVE")
    void create_holdsStock() throws Exception {
        create(createBody(1, "30", 30))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.total").value(100.0))
                .andExpect(jsonPath("$.paid").value(30.0))
                .andExpect(jsonPath("$.remaining").value(70.0))
                .andExpect(jsonPath("$.reference", org.hamcrest.Matchers.startsWith("RES-")))
                .andExpect(jsonPath("$.payments.length()").value(1));
        assertThat(stock(v100)).isEqualTo(4);   // retenu
    }

    @Test
    @DisplayName("Stock insuffisant a la creation -> 409")
    void create_insufficientStock_conflict() throws Exception {
        create(createBody(6, "0", 30)).andExpect(status().isConflict());
        assertThat(stock(v100)).isEqualTo(5);   // rien retenu
    }

    @Test
    @DisplayName("Solder par versements -> COMPLETED, CA reconnu (vente creee), stock NON re-decremente")
    void payToCompletion_recognizesRevenue() throws Exception {
        String ref = extractReference(create(createBody(1, "40", 30)));
        Long id = reservationRepository.findAll().stream()
                .filter(r -> r.getReference().equals(ref)).findFirst().orElseThrow().getId();

        mockMvc.perform(post("/api/reservations/{id}/payments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":60,\"method\":\"ESPECES\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.saleId").isNotEmpty());

        Reservation r = reservationRepository.findById(id).orElseThrow();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.COMPLETED);
        assertThat(r.getClosedAt()).isNotNull();
        assertThat(r.getSale()).isNotNull();
        // CA reconnu = vente au total des articles (100), stock retenu une seule fois (reste a 4).
        assertThat(saleRepository.findById(r.getSale().getId()).orElseThrow().getTotalAmount())
                .isEqualByComparingTo("100.00");
        assertThat(stock(v100)).isEqualTo(4);
    }

    @Test
    @DisplayName("Versement superieur au reste du -> 400")
    void overpayment_badRequest() throws Exception {
        String ref = extractReference(create(createBody(1, "0", 30)));
        Long id = idOf(ref);
        mockMvc.perform(post("/api/reservations/{id}/payments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":150,\"method\":\"ESPECES\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Versement sur une reservation SOLDEE -> 409")
    void payOnCompleted_conflict() throws Exception {
        String ref = extractReference(create(createBody(1, "100", 30)));   // soldee des la creation
        Long id = idOf(ref);
        assertThat(reservationRepository.findById(id).orElseThrow().getStatus())
                .isEqualTo(ReservationStatus.COMPLETED);
        mockMvc.perform(post("/api/reservations/{id}/payments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10,\"method\":\"ESPECES\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Annuler : CANCELLED, stock rendu (4->5), acompte retenu (B6 flag)")
    void cancel_returnsStock_forfeitsDeposit() throws Exception {
        String ref = extractReference(create(createBody(1, "30", 30)));
        Long id = idOf(ref);
        assertThat(stock(v100)).isEqualTo(4);

        mockMvc.perform(post("/api/reservations/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.depositForfeited").value(true));
        assertThat(stock(v100)).isEqualTo(5);   // rendu
    }

    @Test
    @DisplayName("Expiration (job) : echeance depassee -> EXPIRED, stock rendu, acompte retenu")
    void expireOverdue_returnsStock_forfeits() throws Exception {
        String ref = extractReference(create(createBody(1, "20", 30)));
        Long id = idOf(ref);
        assertThat(stock(v100)).isEqualTo(4);

        // Force l'echeance dans le passe puis lance le job.
        Reservation r = reservationRepository.findById(id).orElseThrow();
        r.setDueDate(LocalDateTime.now().minusDays(1));
        reservationRepository.save(r);

        int expired = reservationService.expireOverdue();
        assertThat(expired).isEqualTo(1);

        Reservation after = reservationRepository.findById(id).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(after.isDepositForfeited()).isTrue();
        assertThat(after.getClosedAt()).isNotNull();
        assertThat(stock(v100)).isEqualTo(5);   // rendu
    }

    @Test
    @DisplayName("Echeance proche : due-soon liste la reservation a <=4j, pas celle a 30j")
    void dueSoon_listsNearExpiry() throws Exception {
        String near = extractReference(create(createBody(1, "0", 2)));    // echeance dans 2 jours
        String far = extractReference(create(createBody(1, "0", 30)));    // echeance dans 30 jours

        // Seule la reservation a echeance proche apparait (far a 30j est exclue).
        mockMvc.perform(get("/api/reservations/due-soon"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reference").value(near))
                .andExpect(jsonPath("$[0].dueSoon").value(true))
                .andExpect(jsonPath("$[?(@.reference=='" + far + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Concurrence : 2 reservations sur le dernier exemplaire -> une seule reussit, stock 0")
    void concurrentReservations_lastItem_onlyOneSucceeds() throws Exception {
        Category cat = categoryRepository.findByName("Homme").orElseThrow();
        Color rouge = Fixtures.color(colorRepository, "Rouge");
        Long last = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, rouge, "RLAST", "L", 1, 0, "50.00").getId();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger conflict = new AtomicInteger();
        AtomicInteger unexpected = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    reservationService.create(new ReservationCreateRequest(
                            "Client", null, List.of(new ReservationItemRequest(last, 1)),
                            BigDecimal.ZERO, "ESPECES", 30), adminId);
                    success.incrementAndGet();
                } catch (BusinessException e) {
                    conflict.incrementAndGet();
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

        assertThat(unexpected.get()).isZero();
        assertThat(success.get()).isEqualTo(1);
        assertThat(conflict.get()).isEqualTo(1);
        assertThat(stock(last)).isZero();
    }

    // --------------------------- helpers ---------------------------

    private String extractReference(ResultActions ra) throws Exception {
        String json = ra.andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(json, "$.reference");
    }

    private Long idOf(String reference) {
        return reservationRepository.findAll().stream()
                .filter(r -> r.getReference().equals(reference)).findFirst().orElseThrow().getId();
    }
}
