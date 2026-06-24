package com.smartboutique.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.ReturnRequest;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.PaymentMethod;
import com.smartboutique.entity.Product;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ReturnRepository;
import com.smartboutique.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.smartboutique.support.AbstractPostgresIT;
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

/**
 * Tests d'integration des ventes et retours (Phase 4).
 * Le vendeur est l'administrateur seede (principal reel charge via @WithUserDetails).
 */
@AutoConfigureMockMvc
@WithUserDetails(value = "admin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class SaleReturnIntegrationTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SaleRepository saleRepository;
    @Autowired
    private ReturnRepository returnRepository;

    private Long productId;

    @BeforeEach
    void setUp() {
        // Ordre de nettoyage : retours -> ventes (cascade lignes) -> produits.
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();

        Category category = categoryRepository.findByName("Homme")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Homme").build()));
        Product p = productRepository.save(Product.builder()
                .reference("REF-VENTE").name("Produit vente").category(category)
                .purchasePrice(new BigDecimal("20.00")).salePrice(new BigDecimal("50.00"))
                .quantity(10).seuilAlerte(3).qrCode("REF-VENTE").build());
        this.productId = p.getId();
    }

    private Product createProduct(String ref, int qty, String salePrice, int seuil) {
        return productRepository.save(Product.builder()
                .reference(ref).name(ref).category(categoryRepository.findByName("Homme").orElseThrow())
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal(salePrice))
                .quantity(qty).seuilAlerte(seuil).qrCode(ref).build());
    }

    private int currentStock(Long id) {
        return productRepository.findById(id).orElseThrow().getQuantity();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private SaleRequest sale(List<SaleItemRequest> items, String discount) {
        return new SaleRequest(items, PaymentMethod.ESPECES,
                discount == null ? null : new BigDecimal(discount));
    }

    @Test
    @DisplayName("Vente normale : 201, total correct, stock decremente")
    void createSale_normal_decrementsStock() throws Exception {
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 3)), null);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(150.00))
                .andExpect(jsonPath("$.totalAmount").value(150.00))
                .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))
                .andExpect(jsonPath("$.items[0].totalPrice").value(150.00));

        assertThat(currentStock(productId)).isEqualTo(7);
    }

    @Test
    @DisplayName("Stock insuffisant : 409 et rollback (stock inchange)")
    void createSale_insufficientStock_conflictAndRollback() throws Exception {
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 11)), null);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Stock insuffisant")));

        // Aucun changement : rien n'a ete enregistre.
        assertThat(currentStock(productId)).isEqualTo(10);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    @DisplayName("Vente multi-articles : echec sur un article -> rollback complet (tout ou rien)")
    void createSale_multiItem_rollbackOnFailure() throws Exception {
        Product b = createProduct("REF-B", 2, "30.00", 1);
        SaleRequest req = sale(List.of(
                new SaleItemRequest(productId, 5),   // OK (stock 10)
                new SaleItemRequest(b.getId(), 5)),  // KO (stock 2)
                null);

        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isConflict());

        // Le premier article ne doit pas avoir ete decremente.
        assertThat(currentStock(productId)).isEqualTo(10);
        assertThat(currentStock(b.getId())).isEqualTo(2);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    @DisplayName("Prix capture au moment de la vente : un changement de prix ne reecrit pas l'historique")
    void unitPrice_capturedAtSaleTime() throws Exception {
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 2)), null);
        MvcResult result = mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated())
                .andReturn();
        long saleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        // Modification du prix du produit APRES la vente.
        Product p = productRepository.findById(productId).orElseThrow();
        p.setSalePrice(new BigDecimal("99.00"));
        productRepository.save(p);

        mockMvc.perform(get("/api/sales/{id}", saleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitPrice").value(50.00))   // prix d'origine conserve
                .andExpect(jsonPath("$.totalAmount").value(100.00));
    }

    @Test
    @DisplayName("Remise valide : total = sous-total - remise")
    void saleWithDiscount_ok() throws Exception {
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 2)), "30.00"); // 100 - 30
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subtotal").value(100.00))
                .andExpect(jsonPath("$.discount").value(30.00))
                .andExpect(jsonPath("$.totalAmount").value(70.00));
    }

    @Test
    @DisplayName("Remise superieure au total : 400 (total negatif interdit)")
    void saleWithDiscount_negativeTotal_badRequest() throws Exception {
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 1)), "60.00"); // 50 - 60
        mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isBadRequest());
        assertThat(currentStock(productId)).isEqualTo(10); // rollback
    }

    @Test
    @DisplayName("Retour : reincremente le stock")
    void createReturn_reincrementsStock() throws Exception {
        // Vente de 4 -> stock 6
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 4)), null);
        MvcResult result = mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated()).andReturn();
        long saleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        assertThat(currentStock(productId)).isEqualTo(6);

        // Retour de 2 -> stock 8
        ReturnRequest ret = new ReturnRequest(saleId, productId, 2, "Taille incorrecte");
        mockMvc.perform(post("/api/returns").contentType(MediaType.APPLICATION_JSON).content(json(ret)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(2));

        assertThat(currentStock(productId)).isEqualTo(8);
    }

    @Test
    @DisplayName("Retour superieur au vendu : 409")
    void createReturn_exceedingSold_conflict() throws Exception {
        SaleRequest req = sale(List.of(new SaleItemRequest(productId, 2)), null);
        MvcResult result = mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated()).andReturn();
        long saleId = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        ReturnRequest ret = new ReturnRequest(saleId, productId, 3, "Trop"); // vendu 2
        mockMvc.perform(post("/api/returns").contentType(MediaType.APPLICATION_JSON).content(json(ret)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("retournable")));

        assertThat(currentStock(productId)).isEqualTo(8); // inchange par rapport a apres-vente (10-2)
    }

    @Test
    @DisplayName("Recherche par contenu QR (reference) : produit trouve, sinon 404")
    void searchByQr() throws Exception {
        mockMvc.perform(get("/api/products/by-qr").param("code", "REF-VENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value("REF-VENTE"));

        mockMvc.perform(get("/api/products/by-qr").param("code", "INEXISTANT"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Liste des produits sous le seuil / en rupture")
    void lowStock_listing() throws Exception {
        createProduct("REF-LOW", 2, "10.00", 5);    // sous le seuil
        createProduct("REF-RUPT", 0, "10.00", 1);   // en rupture
        // REF-VENTE : quantite 10, seuil 3 -> sain (ne doit pas apparaitre)

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.reference=='REF-LOW')]").exists())
                .andExpect(jsonPath("$[?(@.reference=='REF-RUPT')]").exists())
                .andExpect(jsonPath("$[?(@.reference=='REF-VENTE')]").doesNotExist());
    }
}
