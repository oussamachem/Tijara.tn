package com.smartboutique.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartboutique.dto.SizeRequest;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SizeRepository;
import com.smartboutique.support.AbstractPostgresIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Catalogue de tailles gere (CRUD, unicite insensible casse, ordre, securite). */
@AutoConfigureMockMvc
class SizeIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private com.smartboutique.repository.SaleRepository saleRepository;
    @Autowired private com.smartboutique.repository.ReturnRepository returnRepository;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        sizeRepository.deleteAll();
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    private Long createSize(String label, Integer position) throws Exception {
        String body = mockMvc.perform(post("/api/admin/sizes").with(user("admin").roles("SHOP_OWNER"))
                        .contentType(MediaType.APPLICATION_JSON).content(json(new SizeRequest(label, position))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asLong();
    }

    @Test
    @WithMockUser(roles = "SHOP_OWNER")
    @DisplayName("Creation S/M/L/XL/XXL + doublon insensible a la casse -> 409")
    void createAndUniqueness() throws Exception {
        for (String label : new String[]{"S", "M", "L", "XL", "XXL"}) {
            mockMvc.perform(post("/api/admin/sizes")
                            .contentType(MediaType.APPLICATION_JSON).content(json(new SizeRequest(label, null))))
                    .andExpect(status().isCreated());
        }
        mockMvc.perform(post("/api/admin/sizes")
                        .contentType(MediaType.APPLICATION_JSON).content(json(new SizeRequest("  m  ", null))))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/sizes")).andExpect(jsonPath("$.length()").value(5));
    }

    @Test
    @WithMockUser(roles = "SHOP_VENDOR")
    @DisplayName("Catalogue trie par position (nulls en dernier) puis libelle")
    void orderedByPosition() throws Exception {
        createSize("L", 3);
        createSize("S", 1);
        createSize("M", 2);
        createSize("Unique", null);
        mockMvc.perform(get("/api/sizes"))
                .andExpect(jsonPath("$[0].label").value("S"))
                .andExpect(jsonPath("$[1].label").value("M"))
                .andExpect(jsonPath("$[2].label").value("L"))
                .andExpect(jsonPath("$[3].label").value("Unique"));
    }

    @Test
    @WithMockUser(roles = "SHOP_OWNER")
    @DisplayName("Suppression d'une taille inutilisee : 204")
    void deleteUnused_noContent() throws Exception {
        Long id = createSize("Temp", null);
        mockMvc.perform(delete("/api/admin/sizes/{id}", id)).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "SHOP_VENDOR")
    @DisplayName("Securite : VENDOR en ecriture -> 403")
    void vendorForbidden() throws Exception {
        mockMvc.perform(post("/api/admin/sizes")
                        .contentType(MediaType.APPLICATION_JSON).content(json(new SizeRequest("Z", null))))
                .andExpect(status().isForbidden());
    }
}
