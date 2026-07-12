package com.smartboutique.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.ImageReorderRequest;
import com.smartboutique.dto.ProductRequest;
import com.smartboutique.dto.VariantCellRequest;
import com.smartboutique.repository.*;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Galerie photos au niveau produit (modele variantes) : upload/cap/suppression+fichier/reordre/cascade/securite. */
@AutoConfigureMockMvc
class ProductImageIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductImageRepository productImageRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;

    @Value("${app.uploads-dir}")
    private String uploadsDir;

    private Long colorId, sizeId, catId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        catId = Fixtures.category(categoryRepository, "Homme").getId();
        colorId = Fixtures.color(colorRepository, "Bleu").getId();
        sizeId = Fixtures.size(sizeRepository, "M").getId();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private Path fileOf(String url) {
        return Paths.get(uploadsDir).resolve(url.substring("/uploads/".length())).normalize();
    }

    /** Cree un produit avec UNE variante (couleur+taille) et renvoie son id. */
    private Long createProduct(String reference) throws Exception {
        var req = new ProductRequest(reference, "Produit", "desc", catId,
                new BigDecimal("20.00"), new BigDecimal("49.90"),
                List.of(new VariantCellRequest(colorId, sizeId, 5, 0)));
        String body = mockMvc.perform(post("/api/admin/products").with(user("admin").roles("SHOP_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    private MockMultipartFile png(String name) {
        return new MockMultipartFile("files", name, "image/png", ("PNG-" + name).getBytes());
    }

    private JsonNode uploadImages(Long pid, MockMultipartFile... files) throws Exception {
        MockMultipartHttpServletRequestBuilder req = multipart("/api/admin/products/{id}/images", pid);
        Stream.of(files).forEach(req::file);
        String body = mockMvc.perform(req.with(user("admin").roles("SHOP_OWNER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    @Test
    @DisplayName("Upload multiple : 3 images -> positions 0/1/2, couverture = position 0 (imageUrl)")
    void uploadMultiple_positionsAndCover() throws Exception {
        Long pid = createProduct("REF-IMG3");
        JsonNode p = uploadImages(pid, png("a.png"), png("b.png"), png("c.png"));
        assertThat(p.get("images")).hasSize(3);
        assertThat(p.get("imageUrl").asText()).isEqualTo(p.get("images").get(0).get("url").asText());
        for (JsonNode img : p.get("images")) {
            assertThat(Files.exists(fileOf(img.get("url").asText()))).isTrue();
        }
    }

    @Test
    @DisplayName("Au-dela du plafond (6) : 400 et aucun fichier ecrit")
    void uploadBeyondCap_isBadRequest() throws Exception {
        Long pid = createProduct("REF-CAP");
        mockMvc.perform(multipart("/api/admin/products/{id}/images", pid)
                        .file(png("1")).file(png("2")).file(png("3"))
                        .file(png("4")).file(png("5")).file(png("6")).file(png("7"))
                        .with(user("admin").roles("SHOP_OWNER")))
                .andExpect(status().isBadRequest());
        assertThat(productImageRepository.countByProductId(pid)).isZero();
    }

    @Test
    @DisplayName("Suppression de la couverture : la suivante devient couverture, fichier disque supprime")
    void deleteCover_nextBecomesCover_fileRemoved() throws Exception {
        Long pid = createProduct("REF-DEL");
        JsonNode p = uploadImages(pid, png("a.png"), png("b.png"), png("c.png"));
        long coverId = p.get("images").get(0).get("id").asLong();
        Path coverFile = fileOf(p.get("images").get(0).get("url").asText());
        String secondUrl = p.get("images").get(1).get("url").asText();

        String body = mockMvc.perform(delete("/api/admin/products/{id}/images/{imageId}", pid, coverId)
                        .with(user("admin").roles("SHOP_OWNER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode after = objectMapper.readTree(body);
        assertThat(after.get("images")).hasSize(2);
        assertThat(after.get("imageUrl").asText()).isEqualTo(secondUrl);
        assertThat(Files.exists(coverFile)).isFalse();
    }

    @Test
    @DisplayName("Reordre : la liste d'ids fixe les positions (0 = couverture)")
    void reorder_updatesPositions() throws Exception {
        Long pid = createProduct("REF-ORD");
        JsonNode p = uploadImages(pid, png("a.png"), png("b.png"), png("c.png"));
        long id0 = p.get("images").get(0).get("id").asLong();
        long id1 = p.get("images").get(1).get("id").asLong();
        long id2 = p.get("images").get(2).get("id").asLong();
        String body = mockMvc.perform(put("/api/admin/products/{id}/images/order", pid)
                        .with(user("admin").roles("SHOP_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ImageReorderRequest(List.of(id2, id0, id1)))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode after = objectMapper.readTree(body);
        assertThat(after.get("images").get(0).get("id").asLong()).isEqualTo(id2);
    }

    @Test
    @DisplayName("Cascade : supprimer le produit supprime ses images (DB + fichiers)")
    void deleteProduct_cascadesImagesAndFiles() throws Exception {
        Long pid = createProduct("REF-CASCADE");
        JsonNode p = uploadImages(pid, png("a.png"), png("b.png"));
        Path f0 = fileOf(p.get("images").get(0).get("url").asText());
        mockMvc.perform(delete("/api/admin/products/{id}", pid).with(user("admin").roles("SHOP_OWNER")))
                .andExpect(status().isNoContent());
        assertThat(productImageRepository.countByProductId(pid)).isZero();
        assertThat(Files.exists(f0)).isFalse();
    }

    @Test
    @DisplayName("Securite : VENDOR en upload -> 403")
    void vendor_cannotUpload_isForbidden() throws Exception {
        Long pid = createProduct("REF-SEC");
        mockMvc.perform(multipart("/api/admin/products/{id}/images", pid)
                        .file(png("a.png"))
                        .with(user("vendor").roles("SHOP_VENDOR")))
                .andExpect(status().isForbidden());
    }
}
