package com.smartboutique.dashboard;

import com.smartboutique.dto.ReturnRequest;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.entity.*;
import com.smartboutique.repository.*;
import com.smartboutique.service.ReturnService;
import com.smartboutique.service.SaleService;
import com.smartboutique.support.AbstractPostgresIT;
import com.smartboutique.support.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Tableau de bord et historique au grain VARIANTE (Phase 9), donnees re-baselinees. */
@AutoConfigureMockMvc
@WithUserDetails(value = "admin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class DashboardHistoryIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ColorRepository colorRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SaleService saleService;
    @Autowired private ReturnService returnService;

    private Long adminId;
    private Long p1v, p2v;
    private Long saleAId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.findByEmail("seller2@smartboutique.com").ifPresent(userRepository::delete);

        adminId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
        Category cat = Fixtures.category(categoryRepository, "Homme", SizeType.LETTER);
        Color bleu = Fixtures.color(colorRepository, "Bleu");

        p1v = Fixtures.variant(productRepository, variantRepository, cat, bleu, "P1", "M", 10, 3, "50.00").getId();
        p2v = Fixtures.variant(productRepository, variantRepository, cat, bleu, "P2", "M", 2, 5, "30.00").getId();
        Fixtures.variant(productRepository, variantRepository, cat, bleu, "P3", "M", 0, 1, "20.00"); // rupture

        saleAId = sale(adminId, List.of(new SaleItemRequest(p1v, 2), new SaleItemRequest(p2v, 1))); // 130
        sale(adminId, List.of(new SaleItemRequest(p1v, 3)));                                         // 150
        returnService.createReturn(new ReturnRequest(saleAId, p1v, 1, "Test retour"));               // -50
    }

    private Long sale(Long sellerId, List<SaleItemRequest> items) {
        SaleResponse r = saleService.createSale(new SaleRequest(items, PaymentMethod.ESPECES, null), sellerId);
        return r.id();
    }

    @Test
    @DisplayName("Tableau de bord : chiffres coherents (grain variante, CA net)")
    void dashboard_figuresAreConsistent() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(3))
                .andExpect(jsonPath("$.totalStock").value(7))            // P1=6, P2=1, P3=0
                .andExpect(jsonPath("$.lowStockVariants").value(2))      // P2 (1<=5), P3 (0<=1)
                .andExpect(jsonPath("$.lowStockProducts").value(2))
                .andExpect(jsonPath("$.todaySalesCount").value(2))
                .andExpect(jsonPath("$.todayGrossRevenue").value(280.00))
                .andExpect(jsonPath("$.todayReturnsValue").value(50.00))
                .andExpect(jsonPath("$.todayNetRevenue").value(230.00))
                .andExpect(jsonPath("$.topSellingProducts[0].reference").value("P1"))
                .andExpect(jsonPath("$.topSellingProducts[0].quantitySold").value(5))
                .andExpect(jsonPath("$.topSellingProducts[1].reference").value("P2"))
                .andExpect(jsonPath("$.topSellingProducts[1].quantitySold").value(1));
    }

    @Test
    @DisplayName("CA net : un retour supplementaire le reduit")
    void dashboard_netRevenue_reflectsAdditionalReturn() throws Exception {
        returnService.createReturn(new ReturnRequest(saleAId, p1v, 1, "Second retour")); // total retours = 2x50
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayReturnsValue").value(100.00))
                .andExpect(jsonPath("$.todayNetRevenue").value(180.00));
    }

    @Test
    @DisplayName("Historique ventes filtre par periode")
    void salesHistory_filteredByPeriod() throws Exception {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        mockMvc.perform(get("/api/admin/sales").param("from", today).param("to", today))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/admin/sales").param("from", tomorrow).param("to", tomorrow))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Historique ventes filtre par vendeur")
    void salesHistory_filteredBySeller() throws Exception {
        User seller2 = userRepository.save(User.builder()
                .fullName("Vendeur 2").email("seller2@smartboutique.com")
                .password(passwordEncoder.encode("x")).role(Role.VENDEUR).active(true).build());
        sale(seller2.getId(), List.of(new SaleItemRequest(p1v, 1)));

        mockMvc.perform(get("/api/admin/sales").param("sellerId", adminId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/admin/sales").param("sellerId", seller2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sellerName").value("Vendeur 2"));
    }

    @Test
    @DisplayName("Detail d'une vente : lignes variante, total")
    void saleDetail() throws Exception {
        mockMvc.perform(get("/api/sales/{id}", saleAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(130.00))
                .andExpect(jsonPath("$.sellerName").value("Administrateur"))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    @DisplayName("Vue des retours : liste filtrable par periode")
    void returnsHistory() throws Exception {
        String today = LocalDate.now().toString();
        mockMvc.perform(get("/api/admin/returns").param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].productName").value("P1"))
                .andExpect(jsonPath("$.content[0].quantity").value(1));
    }

    @Test
    @DisplayName("GET /api/sales/mine : seulement les ventes de l'appelant")
    void myHistory_returnsOnlyCallerSales() throws Exception {
        User seller2 = userRepository.save(User.builder()
                .fullName("Vendeur 2").email("seller2@smartboutique.com")
                .password(passwordEncoder.encode("x")).role(Role.VENDEUR).active(true).build());
        sale(seller2.getId(), List.of(new SaleItemRequest(p1v, 1)));

        mockMvc.perform(get("/api/sales/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].sellerName").value("Administrateur"));
    }

    @Test
    @WithMockUser(roles = "VENDEUR")
    @DisplayName("Acces dashboard refuse a un VENDEUR : 403")
    void dashboard_forbiddenForSeller() throws Exception {
        mockMvc.perform(get("/api/dashboard")).andExpect(status().isForbidden());
    }
}
