package com.smartboutique.credit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.entity.*;
import com.smartboutique.repository.*;
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
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Module Credits sur PostgreSQL reel : stock, argent (BigDecimal), garde-fous, annulation, CA, securite. */
@AutoConfigureMockMvc
@WithUserDetails(value = "admin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class CreditIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private CreditRepository creditRepository;
    @Autowired private CreditPaymentRepository paymentRepository;
    @Autowired private CustomerRepository customerRepository;

    private Long variantId;   // salePrice 25, stock 5

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        creditRepository.deleteAll();
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        customerRepository.deleteAll();
        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color bleu = Fixtures.color(colorRepository, "Bleu");
        variantId = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, bleu,
                "CRED", "M", 5, 0, "25.00").getId();
    }

    /** Les credits referencent des ventes : on les purge pour ne pas bloquer le nettoyage des autres tests. */
    @AfterEach
    void tearDown() {
        paymentRepository.deleteAll();
        creditRepository.deleteAll();
    }

    /** Cree un credit (newCustomer + 1 ligne qty + acompte) et renvoie le JSON. */
    private MvcResult createCredit(String customer, int qty, String downPayment) throws Exception {
        String body = "{\"newCustomer\":{\"name\":\"" + customer + "\",\"phone\":\"123\"},"
                + "\"items\":[{\"variantId\":" + variantId + ",\"quantity\":" + qty + "}],"
                + "\"paymentMethod\":\"ESPECES\",\"downPayment\":" + downPayment + "}";
        return mockMvc.perform(post("/api/admin/credits").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
    }

    private int stock() {
        return variantRepository.findById(variantId).orElseThrow().getQuantity();
    }

    @Test
    @DisplayName("Creation credit = vente : stock variante decremente (5 -> 3) via le verrou")
    void create_decrementsStock() throws Exception {
        createCredit("Ali", 2, "0");
        assertThat(stock()).isEqualTo(3);
        assertThat(creditRepository.count()).isEqualTo(1);
        assertThat(saleRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Stock insuffisant -> 409 et RIEN cree (rollback : ni credit, ni client, ni vente)")
    void create_insufficientStock_rollback() throws Exception {
        String body = "{\"newCustomer\":{\"name\":\"Trop\"},"
                + "\"items\":[{\"variantId\":" + variantId + ",\"quantity\":99}],"
                + "\"paymentMethod\":\"ESPECES\",\"downPayment\":0}";
        mockMvc.perform(post("/api/admin/credits").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        assertThat(stock()).isEqualTo(5);
        assertThat(creditRepository.count()).isZero();
        assertThat(saleRepository.count()).isZero();
        assertThat(customerRepository.count()).isZero();   // le client cree avant la vente est rollback
    }

    @Test
    @DisplayName("Argent exact : total 50, acompte 20 -> reste 30 (PARTIAL) ; paiement 30 -> reste 0 (PAID)")
    void money_exact() throws Exception {
        // total = 25.00 x 2 = 50.00 ; acompte 20
        MvcResult res = createCredit("Sami", 2, "20");
        var node = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(node.get("total").asDouble()).isEqualTo(50.0);
        assertThat(node.get("paid").asDouble()).isEqualTo(20.0);
        assertThat(node.get("remaining").asDouble()).isEqualTo(30.0);
        assertThat(node.get("status").asText()).isEqualTo("PARTIAL");
        long creditId = node.get("id").asLong();

        // Paiement de 30 -> solde
        mockMvc.perform(post("/api/admin/credits/{id}/payments", creditId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Garde-fous : paiement > reste -> 400 ; paiement <= 0 -> 400 ; double-clic plein ne double pas")
    void guards() throws Exception {
        long creditId = objectMapper.readTree(createCredit("Nadia", 2, "0").getResponse().getContentAsString())
                .get("id").asLong();   // total 50, reste 50

        // > reste
        mockMvc.perform(post("/api/admin/credits/{id}/payments", creditId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":60}"))
                .andExpect(status().isBadRequest());
        // <= 0
        mockMvc.perform(post("/api/admin/credits/{id}/payments", creditId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
        // double-clic d'un paiement plein : 1er OK (reste 0), 2e refuse
        mockMvc.perform(post("/api/admin/credits/{id}/payments", creditId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/credits/{id}/payments", creditId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":50}"))
                .andExpect(status().isBadRequest());
        assertThat(paymentRepository.sumPaidByCredit(creditId).doubleValue()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("CA accrual : la vente a credit compte en plein le jour J (dashboard ventes inchange)")
    void ca_accrual() throws Exception {
        createCredit("Karim", 2, "20");   // total 50, encaisse 20
        // Dashboard VENTES : le total (50) compte en brut le jour meme, net 50 (aucun retour).
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.todayGrossRevenue").value(50.0))
                .andExpect(jsonPath("$.todayNetRevenue").value(50.0));
        // Dashboard CREDITS : total 50, encaisse 20, restant 30.
        mockMvc.perform(get("/api/admin/credits/dashboard"))
                .andExpect(jsonPath("$.creditsCount").value(1))
                .andExpect(jsonPath("$.totalAmount").value(50.0))
                .andExpect(jsonPath("$.collected").value(20.0))
                .andExpect(jsonPath("$.outstanding").value(30.0));
    }

    @Test
    @DisplayName("Annulation : restock (3 -> 5) + CA net restaure (reversal) + exclu de la tresorerie")
    void cancel_restocksAndReversesCA() throws Exception {
        long creditId = objectMapper.readTree(createCredit("Leila", 2, "20").getResponse().getContentAsString())
                .get("id").asLong();
        assertThat(stock()).isEqualTo(3);

        mockMvc.perform(post("/api/admin/credits/{id}/cancel", creditId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelled").value(true))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(stock()).isEqualTo(5);   // stock reintegre
        // CA net restaure : brut 50 - reversal 50 = 0 (le stock et le CA ne sont pas fausses).
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.todayNetRevenue").value(0));
        // Tresorerie credits : le credit annule est exclu.
        mockMvc.perform(get("/api/admin/credits/dashboard"))
                .andExpect(jsonPath("$.creditsCount").value(0))
                .andExpect(jsonPath("$.outstanding").value(0));
    }

    @Test
    @DisplayName("Filtres : par statut (PARTIAL vs PAID) -> sous-ensembles corrects")
    void filter_byStatus() throws Exception {
        long c1 = objectMapper.readTree(createCredit("A", 1, "0").getResponse().getContentAsString()).get("id").asLong(); // 25, UNPAID
        long c2 = objectMapper.readTree(createCredit("B", 1, "25").getResponse().getContentAsString()).get("id").asLong(); // 25 paye, PAID
        mockMvc.perform(get("/api/admin/credits").param("status", "PAID"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) c2));
        mockMvc.perform(get("/api/admin/credits").param("status", "UNPAID"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) c1));
    }

    @Test
    @DisplayName("Securite : VENDOR sur le module credits -> 403")
    void security_vendorForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/credits").with(user("v").roles("VENDEUR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/credits").with(user("v").roles("VENDEUR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
