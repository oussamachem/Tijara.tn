package com.smartboutique.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Feature A — Ticket cadeau : tenders multiples, usage unique global, expiration, denominations. */
@AutoConfigureMockMvc
@WithUserDetails(value = "admin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class GiftTicketIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private SizeRepository sizeRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;

    private Long v65;  // prix 65
    private Long v40;  // prix 40
    private final String future = LocalDate.now().plusYears(1).toString();

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();  // cascade -> sale_payments
        variantRepository.deleteAll();
        productRepository.deleteAll();
        Category cat = Fixtures.category(categoryRepository, "Homme");
        Color bleu = Fixtures.color(colorRepository, "Bleu");
        v65 = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, bleu, "T65", "M", 100, 0, "65.00").getId();
        v40 = Fixtures.variant(productRepository, variantRepository, sizeRepository, cat, bleu, "T40", "L", 100, 0, "40.00").getId();
    }

    private org.springframework.test.web.servlet.ResultActions sale(String body) throws Exception {
        return mockMvc.perform(post("/api/sales").contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private int stock(Long id) { return variantRepository.findById(id).orElseThrow().getQuantity(); }

    @Test
    @DisplayName("Ticket 20 (PLUXEE) + 45 especes pour total 65 -> 201, CA=65 (articles), stock decremente")
    void ticketPlusCash_ok() throws Exception {
        String body = "{\"items\":[{\"variantId\":" + v65 + ",\"quantity\":1}],\"payments\":["
                + "{\"method\":\"TICKET_CADEAU\",\"amount\":20,\"issuer\":\"PLUXEE\",\"ticketCode\":\"CODE-X\",\"ticketSerial\":\"640549\",\"ticketExpiry\":\"" + future + "\"},"
                + "{\"method\":\"ESPECES\",\"amount\":45}]}";
        sale(body).andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(65.0))   // CA = prix articles, PAS la valeur du ticket
                .andExpect(jsonPath("$.payments.length()").value(2))
                .andExpect(jsonPath("$.change").value(0));
        assertThat(stock(v65)).isEqualTo(99);
    }

    @Test
    @DisplayName("Reutilisation du meme code ticket (2e vente) -> 409 (usage unique global)")
    void ticketReuse_conflict() throws Exception {
        String t = "{\"method\":\"TICKET_CADEAU\",\"amount\":20,\"issuer\":\"PLUXEE\",\"ticketCode\":\"REUSE-1\",\"ticketExpiry\":\"" + future + "\"}";
        sale("{\"items\":[{\"variantId\":" + v65 + ",\"quantity\":1}],\"payments\":[" + t + ",{\"method\":\"ESPECES\",\"amount\":45}]}")
                .andExpect(status().isCreated());
        // meme code sur une NOUVELLE vente
        sale("{\"items\":[{\"variantId\":" + v40 + ",\"quantity\":1}],\"payments\":[" + t + ",{\"method\":\"ESPECES\",\"amount\":20}]}")
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Ticket expire -> 400")
    void ticketExpired_badRequest() throws Exception {
        String past = LocalDate.now().minusDays(1).toString();
        sale("{\"items\":[{\"variantId\":" + v65 + ",\"quantity\":1}],\"payments\":["
                + "{\"method\":\"TICKET_CADEAU\",\"amount\":20,\"issuer\":\"PLUXEE\",\"ticketCode\":\"EXP-1\",\"ticketExpiry\":\"" + past + "\"},"
                + "{\"method\":\"ESPECES\",\"amount\":45}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Denomination invalide (17) -> 400")
    void invalidDenomination_badRequest() throws Exception {
        sale("{\"items\":[{\"variantId\":" + v65 + ",\"quantity\":1}],\"payments\":["
                + "{\"method\":\"TICKET_CADEAU\",\"amount\":17,\"issuer\":\"PLUXEE\",\"ticketCode\":\"DEN-1\"},"
                + "{\"method\":\"ESPECES\",\"amount\":48}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Meme code deux fois dans la meme vente -> 400")
    void duplicateCodeSameSale_badRequest() throws Exception {
        String t = "{\"method\":\"TICKET_CADEAU\",\"amount\":20,\"issuer\":\"PLUXEE\",\"ticketCode\":\"DUP-1\",\"ticketExpiry\":\"" + future + "\"}";
        sale("{\"items\":[{\"variantId\":" + v65 + ",\"quantity\":1}],\"payments\":[" + t + "," + t + ",{\"method\":\"ESPECES\",\"amount\":25}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Tenders < total -> 400")
    void insufficientTenders_badRequest() throws Exception {
        sale("{\"items\":[{\"variantId\":" + v65 + ",\"quantity\":1}],\"payments\":["
                + "{\"method\":\"TICKET_CADEAU\",\"amount\":20,\"issuer\":\"PLUXEE\",\"ticketCode\":\"INS-1\",\"ticketExpiry\":\"" + future + "\"}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ticket 50 pour article 40 -> accepte, PAS de rendu sur ticket (change=0, surplus perdu)")
    void ticketOverTotal_acceptedNoChange() throws Exception {
        sale("{\"items\":[{\"variantId\":" + v40 + ",\"quantity\":1}],\"payments\":["
                + "{\"method\":\"TICKET_CADEAU\",\"amount\":50,\"issuer\":\"PLUXEE\",\"ticketCode\":\"OVER-1\",\"ticketExpiry\":\"" + future + "\"}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalAmount").value(40.0))
                .andExpect(jsonPath("$.change").value(0));   // pas de rendu sur ticket
    }

    @Test
    @DisplayName("Especes 50 pour total 40 -> rendu 10 (rendu uniquement sur la part especes)")
    void cashChange_ok() throws Exception {
        sale("{\"items\":[{\"variantId\":" + v40 + ",\"quantity\":1}],\"payments\":["
                + "{\"method\":\"ESPECES\",\"amount\":50}]}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.change").value(10.0));
    }
}
