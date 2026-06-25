package com.smartboutique.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.ProductRequest;
import com.smartboutique.dto.VariantCellRequest;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.Color;
import com.smartboutique.entity.SizeType;
import com.smartboutique.repository.*;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Categories, couleurs, produits et VARIANTES (Phase 9). */
@AutoConfigureMockMvc
class ProductCategoryIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;

    private Long catId;
    private Long bleu, rouge, vert;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        Category cat = Fixtures.category(categoryRepository, "Homme", SizeType.LETTER);
        catId = cat.getId();
        bleu = Fixtures.color(colorRepository, "Bleu").getId();
        rouge = Fixtures.color(colorRepository, "Rouge").getId();
        vert = Fixtures.color(colorRepository, "Vert").getId();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    /** Construit un ProductRequest = produit cartesien (couleurs x tailles). */
    private ProductRequest cartesian(String ref, List<Long> colors, List<String> sizes, int qty) {
        List<VariantCellRequest> cells = new ArrayList<>();
        for (Long c : colors)
            for (String s : sizes)
                cells.add(new VariantCellRequest(c, s, qty, 0));
        return new ProductRequest(ref, ref, "desc", catId,
                new BigDecimal("20.00"), new BigDecimal("49.90"), cells);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Creation cartesienne : 3 couleurs x 4 tailles = 12 variantes, references+QR uniques")
    void createProduct_cartesian() throws Exception {
        ProductRequest req = cartesian("CHEMISE", List.of(bleu, rouge, vert), List.of("S", "M", "L", "XL"), 5);
        MvcResult res = mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.variants.length()").value(12))
                .andExpect(jsonPath("$.totalStock").value(60))
                .andReturn();
        // References de variantes toutes distinctes et non vides.
        var node = objectMapper.readTree(res.getResponse().getContentAsString());
        var refs = new java.util.HashSet<String>();
        node.get("variants").forEach(v -> refs.add(v.get("reference").asText()));
        assertThat(refs).hasSize(12);
        assertThat(refs).contains("CHEMISE-M-BLEU", "CHEMISE-XL-VERT");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Reference produit en doublon : 409")
    void createProduct_duplicateReference_conflict() throws Exception {
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(json(cartesian("REF-DUP", List.of(bleu), List.of("M"), 3)))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(json(cartesian("REF-DUP", List.of(rouge), List.of("L"), 3)))).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Taille invalide pour la categorie (LETTER + '42') : 400")
    void createProduct_invalidSize_badRequest() throws Exception {
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(json(cartesian("REF-BADSIZE", List.of(bleu), List.of("42"), 3))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("invalide")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Cellule en double dans la matrice (meme couleur+taille) : 400")
    void createProduct_duplicateCell_badRequest() throws Exception {
        var cells = List.of(new VariantCellRequest(bleu, "M", 3, 0), new VariantCellRequest(bleu, "M", 5, 0));
        var req = new ProductRequest("REF-DUPCELL", "x", null, catId,
                new BigDecimal("10"), new BigDecimal("20"), cells);
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Produit sans variante : 400 (invariant)")
    void createProduct_noVariant_badRequest() throws Exception {
        var req = new ProductRequest("REF-EMPTY", "x", null, catId,
                new BigDecimal("10"), new BigDecimal("20"), List.of());
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    @DisplayName("size-options : LETTER -> S..XXL ; SHOE_EU -> 38..46")
    void sizeOptions_byCategory() throws Exception {
        mockMvc.perform(get("/api/categories/{id}/size-options", catId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sizeType").value("LETTER"))
                .andExpect(jsonPath("$.sizes[0]").value("S"))
                .andExpect(jsonPath("$.sizes[4]").value("XXL"));
        Category shoes = Fixtures.category(categoryRepository, "Chaussures", SizeType.SHOE_EU);
        mockMvc.perform(get("/api/categories/{id}/size-options", shoes.getId()))
                .andExpect(jsonPath("$.sizes[0]").value("38"))
                .andExpect(jsonPath("$.sizes[8]").value("46"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Ajout puis retrait de variante ; retrait de la derniere refuse (invariant)")
    void addAndRemoveVariant_invariant() throws Exception {
        // Produit a 1 variante
        MvcResult res = mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                        .content(json(cartesian("REF-VAR", List.of(bleu), List.of("M"), 4))))
                .andExpect(status().isCreated()).andReturn();
        long productId = objectMapper.readTree(res.getResponse().getContentAsString()).get("id").asLong();
        long firstVariant = objectMapper.readTree(res.getResponse().getContentAsString())
                .get("variants").get(0).get("id").asLong();

        // Ajout d'une 2e variante
        MvcResult add = mockMvc.perform(post("/api/admin/products/{id}/variants", productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new com.smartboutique.dto.AddVariantRequest(rouge, "L", 2, 0))))
                .andExpect(status().isCreated()).andReturn();
        long secondVariant = objectMapper.readTree(add.getResponse().getContentAsString()).get("id").asLong();

        // Retrait de la 2e -> OK
        mockMvc.perform(delete("/api/admin/variants/{id}", secondVariant)).andExpect(status().isNoContent());
        // Retrait de la derniere -> 409
        mockMvc.perform(delete("/api/admin/variants/{id}", firstVariant)).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Suppression categorie rattachee a des produits : 409")
    void deleteCategory_withProducts_conflict() throws Exception {
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(json(cartesian("REF-CAT", List.of(bleu), List.of("M"), 3)))).andExpect(status().isCreated());
        mockMvc.perform(delete("/api/admin/categories/{id}", catId)).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Suppression couleur utilisee par une variante : 409")
    void deleteColor_inUse_conflict() throws Exception {
        mockMvc.perform(post("/api/admin/products").contentType(MediaType.APPLICATION_JSON)
                .content(json(cartesian("REF-COL", List.of(bleu), List.of("M"), 3)))).andExpect(status().isCreated());
        mockMvc.perform(delete("/api/admin/colors/{id}", bleu)).andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    @DisplayName("Image PNG du QR Code d'une variante")
    void variantQrImage_returnsPng() throws Exception {
        var v = Fixtures.variant(productRepository, variantRepository,
                categoryRepository.findById(catId).orElseThrow(),
                colorRepository.findById(bleu).orElseThrow(), "REF-QR", "M", 5, 0, "10.00");
        MvcResult result = mockMvc.perform(get("/api/variants/{id}/qrcode", v.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();
        byte[] body = result.getResponse().getContentAsByteArray();
        assertThat(body[0] & 0xFF).isEqualTo(0x89);
        assertThat(body[1]).isEqualTo((byte) 'P');
    }
}
