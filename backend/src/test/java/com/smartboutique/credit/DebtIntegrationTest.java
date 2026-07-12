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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Module Dettes fournisseurs : PAS d'effet stock, PAS d'effet CA, argent exact, garde-fous, suppression, securite. */
@AutoConfigureMockMvc
@WithMockUser(roles = "SHOP_OWNER")
class DebtIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private SupplierDebtRepository debtRepository;
    @Autowired private DebtPaymentRepository debtPaymentRepository;
    @Autowired private SupplierRepository supplierRepository;

    private Long productId;
    private Long variantId;

    @BeforeEach
    void setUp() {
        debtPaymentRepository.deleteAll();
        debtRepository.deleteAll();
        supplierRepository.deleteAll();
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color bleu = Fixtures.color(colorRepository, "Bleu");
        ProductVariant v = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, bleu,
                "DEBTP", "M", 5, 0, "25.00");
        variantId = v.getId();
        productId = v.getProduct().getId();
    }

    @AfterEach
    void tearDown() {
        debtPaymentRepository.deleteAll();
        debtRepository.deleteAll();   // les dettes referencent des produits (FK)
        supplierRepository.deleteAll();
    }

    private int stock() { return variantRepository.findById(variantId).orElseThrow().getQuantity(); }

    /** Cree une dette (nouveau fournisseur + total saisi + lien produit descriptif + acompte). */
    private MvcResult createDebt(String supplier, String total, String down) throws Exception {
        String body = "{\"newSupplier\":{\"name\":\"" + supplier + "\"},"
                + "\"totalAmount\":" + total + ",\"productId\":" + productId
                + ",\"invoiceReference\":\"F-001\",\"downPayment\":" + down + "}";
        return mockMvc.perform(post("/api/admin/debts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
    }

    @Test
    @DisplayName("AUCUN effet stock : dette associee a un produit -> stock variante INCHANGE")
    void noStockEffect() throws Exception {
        assertThat(stock()).isEqualTo(5);
        createDebt("ACME", "100", "0");
        assertThat(stock()).isEqualTo(5);   // le lien produit est purement descriptif
    }

    @Test
    @DisplayName("AUCUN effet CA : le dashboard ventes (brut/net) est identique avant/apres")
    void noCaEffect() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.todayGrossRevenue").value(0))
                .andExpect(jsonPath("$.todayNetRevenue").value(0));
        createDebt("ACME", "100", "30");
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(jsonPath("$.todayGrossRevenue").value(0))
                .andExpect(jsonPath("$.todayNetRevenue").value(0));
    }

    @Test
    @DisplayName("Argent exact : total 100, acompte 30 -> reste 70 PARTIAL ; paiement 70 -> 0 PAID")
    void money_exact() throws Exception {
        var node = objectMapper.readTree(createDebt("ACME", "100", "30").getResponse().getContentAsString());
        assertThat(node.get("total").asDouble()).isEqualTo(100.0);
        assertThat(node.get("remaining").asDouble()).isEqualTo(70.0);
        assertThat(node.get("status").asText()).isEqualTo("PARTIAL");
        long debtId = node.get("id").asLong();

        mockMvc.perform(post("/api/admin/debts/{id}/payments", debtId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":70}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @DisplayName("Garde-fous : paiement > reste -> 400 ; paiement <= 0 -> 400")
    void guards() throws Exception {
        long debtId = objectMapper.readTree(createDebt("ACME", "100", "0").getResponse().getContentAsString())
                .get("id").asLong();
        mockMvc.perform(post("/api/admin/debts/{id}/payments", debtId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":120}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/debts/{id}/payments", debtId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Dashboard dettes : nb / total / paye / restant corrects")
    void dashboard() throws Exception {
        createDebt("A", "100", "30");
        createDebt("B", "50", "0");
        mockMvc.perform(get("/api/admin/debts/dashboard"))
                .andExpect(jsonPath("$.debtsCount").value(2))
                .andExpect(jsonPath("$.totalAmount").value(150.0))
                .andExpect(jsonPath("$.paid").value(30.0))
                .andExpect(jsonPath("$.outstanding").value(120.0));
    }

    @Test
    @DisplayName("Filtre par statut : PAID vs UNPAID -> sous-ensembles")
    void filter_byStatus() throws Exception {
        long unpaid = objectMapper.readTree(createDebt("A", "100", "0").getResponse().getContentAsString()).get("id").asLong();
        long paid = objectMapper.readTree(createDebt("B", "50", "50").getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(get("/api/admin/debts").param("status", "PAID"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) paid));
        mockMvc.perform(get("/api/admin/debts").param("status", "UNPAID"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value((int) unpaid));
    }

    @Test
    @DisplayName("Suppression : bloquee si paiements (409, historique preserve) ; OK si aucun paiement (204)")
    void delete_behaviour() throws Exception {
        long withPay = objectMapper.readTree(createDebt("A", "100", "30").getResponse().getContentAsString()).get("id").asLong();
        long noPay = objectMapper.readTree(createDebt("B", "50", "0").getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(delete("/api/admin/debts/{id}", withPay)).andExpect(status().isConflict());
        assertThat(debtRepository.existsById(withPay)).isTrue();   // historique preserve
        mockMvc.perform(delete("/api/admin/debts/{id}", noPay)).andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Securite : VENDOR -> 403")
    void security_vendorForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/debts").with(user("v").roles("SHOP_VENDOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/debts").with(user("v").roles("SHOP_VENDOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }
}
