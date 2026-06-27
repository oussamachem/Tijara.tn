package com.smartboutique.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.ReturnRequest;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.entity.*;
import com.smartboutique.repository.*;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Ventes et retours au grain VARIANTE (Phase 9). Vendeur = admin seede. */
@AutoConfigureMockMvc
@WithUserDetails(value = "admin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class SaleReturnIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;

    private Long variantId;   // REF-VENTE / M / Bleu, stock 10, prix 50
    private Long productId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();

        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color bleu = Fixtures.color(colorRepository, "Bleu");
        ProductVariant v = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat,bleu,
                "REF-VENTE", "M", 10, 3, "50.00");
        variantId = v.getId();
        productId = v.getProduct().getId();
    }

    private int stock(Long vId) {
        return variantRepository.findById(vId).orElseThrow().getQuantity();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private SaleRequest sale(List<SaleItemRequest> items, String discount) {
        return new SaleRequest(items, PaymentMethod.ESPECES, discount == null ? null : new BigDecimal(discount));
    }

    @Test
    @DisplayName("Vente normale : 201, total correct, stock variante decremente")
    void createSale_normal_decrementsStock() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 3)), null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(150.00))
                .andExpect(jsonPath("$.totalAmount").value(150.00))
                .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
                .andExpect(jsonPath("$.items[0].size").value("M"))
                .andExpect(jsonPath("$.items[0].colorName").value("Bleu"));
        assertThat(stock(variantId)).isEqualTo(7);
    }

    @Test
    @DisplayName("Stock variante insuffisant : 409 et rollback")
    void createSale_insufficientStock_conflictAndRollback() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 11)), null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stock insuffisant")));
        assertThat(stock(variantId)).isEqualTo(10);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    @DisplayName("Vente multi-variantes : echec sur une -> rollback complet")
    void createSale_multiItem_rollbackOnFailure() throws Exception {
        Category cat = categoryRepository.findByName("Homme").orElseThrow();
        Color rouge = Fixtures.color(colorRepository, "Rouge");
        Long vB = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat,rouge, "REF-B", "L", 2, 1, "30.00").getId();

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(
                                new SaleItemRequest(variantId, 5), new SaleItemRequest(vB, 5)), null))))
                .andExpect(status().isConflict());
        assertThat(stock(variantId)).isEqualTo(10);
        assertThat(stock(vB)).isEqualTo(2);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    @DisplayName("Prix capture a la vente : changement de prix produit ne reecrit pas l'historique")
    void unitPrice_capturedAtSaleTime() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 2)), null))))
                .andExpect(status().isCreated()).andReturn();
        long saleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        Product p = productRepository.findById(productId).orElseThrow();
        p.setSalePrice(new BigDecimal("99.00"));
        productRepository.save(p);

        mockMvc.perform(get("/api/sales/{id}", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
                .andExpect(jsonPath("$.totalAmount").value(100.00));
    }

    @Test
    @DisplayName("Remise valide : total = sous-total - remise")
    void saleWithDiscount_ok() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 2)), "30.00"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(100.00))
                .andExpect(jsonPath("$.totalAmount").value(70.00));
    }

    @Test
    @DisplayName("Remise > total : 400")
    void saleWithDiscount_negativeTotal_badRequest() throws Exception {
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 1)), "60.00"))))
                .andExpect(status().isBadRequest());
        assertThat(stock(variantId)).isEqualTo(10);
    }

    @Test
    @DisplayName("Retour : reincremente le stock variante")
    void createReturn_reincrementsStock() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 4)), null))))
                .andExpect(status().isCreated()).andReturn();
        long saleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(stock(variantId)).isEqualTo(6);

        mockMvc.perform(post("/api/returns").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReturnRequest(saleId, variantId, 2, "Taille"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.size").value("M"));
        assertThat(stock(variantId)).isEqualTo(8);
    }

    @Test
    @DisplayName("Retour superieur au vendu : 409")
    void createReturn_exceedingSold_conflict() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON)
                        .content(json(sale(List.of(new SaleItemRequest(variantId, 2)), null))))
                .andExpect(status().isCreated()).andReturn();
        long saleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/returns").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ReturnRequest(saleId, variantId, 3, "Trop"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("retournable")));
    }

    @Test
    @DisplayName("Scan : resolution variante par contenu QR")
    void searchByQr() throws Exception {
        mockMvc.perform(get("/api/variants/by-qr").param("code", "REF-VENTE-M-BLEU"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantReference").value("REF-VENTE-M-BLEU"))
                .andExpect(jsonPath("$.size").value("M"))
                .andExpect(jsonPath("$.productName").value("REF-VENTE"));

        mockMvc.perform(get("/api/variants/by-qr").param("code", "INEXISTANT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Liste des variantes sous seuil / en rupture")
    void lowStock_listing() throws Exception {
        Category cat = categoryRepository.findByName("Homme").orElseThrow();
        Color vert = Fixtures.color(colorRepository, "Vert");
        Fixtures.variant(productRepository, variantRepository, sizeRepository, cat,vert, "REF-LOW", "S", 2, 5, "10.00"); // sous seuil
        Fixtures.variant(productRepository, variantRepository, sizeRepository, cat,vert, "REF-RUPT", "L", 0, 1, "10.00"); // rupture
        // REF-VENTE : 10 <= 3 ? non -> sain

        mockMvc.perform(get("/api/variants/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.variantReference=='REF-LOW-S-VERT')]").exists())
                .andExpect(jsonPath("$[?(@.variantReference=='REF-RUPT-L-VERT')]").exists())
                .andExpect(jsonPath("$[?(@.variantReference=='REF-VENTE-M-BLEU')]").doesNotExist());
    }
}
