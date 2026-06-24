package com.smartboutique.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.ProductRequest;
import com.smartboutique.entity.Category;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.smartboutique.support.AbstractPostgresIT;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration des categories et produits (Phase 3).
 */
@AutoConfigureMockMvc
class ProductCategoryIntegrationTest extends AbstractPostgresIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private com.smartboutique.repository.SaleRepository saleRepository;
    @Autowired
    private com.smartboutique.repository.ReturnRepository returnRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        // Nettoyage en ordre FK-safe (le contexte H2 est partage entre classes de test).
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        Category category = categoryRepository.findByName("Homme")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Homme").build()));
        this.categoryId = category.getId();
    }

    private ProductRequest product(String reference, String name) {
        return new ProductRequest(reference, name, "Description", categoryId,
                "M", "Bleu", new BigDecimal("20.00"), new BigDecimal("49.90"), 10, 3);
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Creation produit : 201 et QR Code genere a partir de la reference")
    void createProduct_generatesQrCode() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(json(product("REF-001", "T-shirt"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.reference").value("REF-001"))
                .andExpect(jsonPath("$.qrCode").value("REF-001"))
                .andExpect(jsonPath("$.categoryName").value("Homme"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Reference en doublon : 409")
    void createProduct_duplicateReference_isConflict() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(json(product("REF-DUP", "Pull"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(json(product("REF-DUP", "Autre"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("reference")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Prix negatif : 400 (validation)")
    void createProduct_invalidPrice_isBadRequest() throws Exception {
        ProductRequest invalid = new ProductRequest("REF-NEG", "X", null, categoryId,
                null, null, new BigDecimal("-1"), new BigDecimal("10"), 5, 0);
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(json(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Suppression d'une categorie rattachee a des produits : 409")
    void deleteCategory_withProducts_isConflict() throws Exception {
        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(json(product("REF-CAT", "Jean"))))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/admin/categories/{id}", categoryId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("produits")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Suppression d'une categorie vide : 204")
    void deleteCategory_empty_isNoContent() throws Exception {
        Category empty = categoryRepository.save(Category.builder().name("Categorie-Vide-Test").build());
        mockMvc.perform(delete("/api/admin/categories/{id}", empty.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    @DisplayName("Recherche paginee filtree par nom")
    void searchProducts_filteredAndPaginated() throws Exception {
        createViaAdmin(product("REF-A1", "Chemise blanche"));
        createViaAdmin(product("REF-A2", "Chemise bleue"));
        createViaAdmin(product("REF-B1", "Pantalon noir"));

        // Filtre name=chemise, taille de page = 1 -> 2 resultats au total, 2 pages.
        mockMvc.perform(get("/api/products")
                        .param("name", "chemise")
                        .param("size", "1")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    @WithMockUser
    @DisplayName("Recuperation de l'image PNG du QR Code")
    void getQrCodeImage_returnsPng() throws Exception {
        String location = createViaAdmin(product("REF-QR", "Casquette"));
        Long id = objectMapper.readTree(location).get("id").asLong();

        MvcResult result = mockMvc.perform(get("/api/products/{id}/qrcode", id))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();

        byte[] body = result.getResponse().getContentAsByteArray();
        // Signature PNG : 0x89 'P' 'N' 'G'
        assertThat(body.length).isGreaterThan(8);
        assertThat(body[0] & 0xFF).isEqualTo(0x89);
        assertThat(body[1]).isEqualTo((byte) 'P');
        assertThat(body[2]).isEqualTo((byte) 'N');
        assertThat(body[3]).isEqualTo((byte) 'G');
    }

    /** Cree un produit via l'endpoint admin (role ADMIN force au niveau de la requete). */
    private String createViaAdmin(ProductRequest request) throws Exception {
        return mockMvc.perform(post("/api/admin/products")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
