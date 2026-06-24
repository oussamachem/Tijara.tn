package com.smartboutique.dashboard;

import com.smartboutique.dto.ReturnRequest;
import com.smartboutique.dto.SaleItemRequest;
import com.smartboutique.dto.SaleRequest;
import com.smartboutique.dto.SaleResponse;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.PaymentMethod;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.Role;
import com.smartboutique.entity.User;
import com.smartboutique.repository.*;
import com.smartboutique.service.ReturnService;
import com.smartboutique.service.SaleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.smartboutique.support.AbstractPostgresIT;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests d'integration du tableau de bord et de l'historique (Phase 5), avec des donnees connues.
 */
@AutoConfigureMockMvc
@WithUserDetails(value = "admin@smartboutique.com", userDetailsServiceBeanName = "customUserDetailsService")
class DashboardHistoryIntegrationTest extends AbstractPostgresIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private SaleRepository saleRepository;
    @Autowired private ReturnRepository returnRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private SaleService saleService;
    @Autowired private ReturnService returnService;

    private Long adminId;
    private Long p1Id, p2Id;
    private Long saleAId;

    @BeforeEach
    void setUp() {
        returnRepository.deleteAll();
        saleRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.findByEmail("seller2@smartboutique.com").ifPresent(userRepository::delete);

        adminId = userRepository.findByEmail("admin@smartboutique.com").orElseThrow().getId();
        Category cat = categoryRepository.findByName("Homme")
                .orElseGet(() -> categoryRepository.save(Category.builder().name("Homme").build()));

        p1Id = newProduct(cat, "P1", 10, "50.00", 3).getId();  // sain
        p2Id = newProduct(cat, "P2", 2, "30.00", 5).getId();   // sous le seuil
        newProduct(cat, "P3", 0, "20.00", 1);                  // rupture

        // Ventes du jour (vendeur = admin).
        saleAId = sale(adminId, List.of(new SaleItemRequest(p1Id, 2), new SaleItemRequest(p2Id, 1))); // total 130
        sale(adminId, List.of(new SaleItemRequest(p1Id, 3)));                                          // total 150
        // P1 : 10 -> 5 ; P2 : 2 -> 1

        // Retour du jour : 1x P1 (valorise a 50) -> P1 : 5 -> 6
        returnService.createReturn(new ReturnRequest(saleAId, p1Id, 1, "Test retour"));
    }

    private Product newProduct(Category cat, String ref, int qty, String price, int seuil) {
        return productRepository.save(Product.builder()
                .reference(ref).name(ref).category(cat)
                .purchasePrice(new BigDecimal("5.00")).salePrice(new BigDecimal(price))
                .quantity(qty).seuilAlerte(seuil).qrCode(ref).build());
    }

    private Long sale(Long sellerId, List<SaleItemRequest> items) {
        SaleResponse r = saleService.createSale(new SaleRequest(items, PaymentMethod.ESPECES, null), sellerId);
        return r.id();
    }

    @Test
    @DisplayName("Tableau de bord : chaque chiffre est coherent (CA net des retours)")
    void dashboard_figuresAreConsistent() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(3))
                .andExpect(jsonPath("$.totalStock").value(7))        // 6 + 1 + 0
                .andExpect(jsonPath("$.lowStockCount").value(2))     // P2 (1<=5) et P3 (0<=1)
                .andExpect(jsonPath("$.todaySalesCount").value(2))
                .andExpect(jsonPath("$.todayGrossRevenue").value(280.00))   // 130 + 150
                .andExpect(jsonPath("$.todayReturnsValue").value(50.00))    // 1 x 50
                .andExpect(jsonPath("$.todayNetRevenue").value(230.00))     // 280 - 50
                // Top ventes par quantite : P1 (5) devant P2 (1)
                .andExpect(jsonPath("$.topSellingProducts[0].reference").value("P1"))
                .andExpect(jsonPath("$.topSellingProducts[0].quantitySold").value(5))
                .andExpect(jsonPath("$.topSellingProducts[1].reference").value("P2"))
                .andExpect(jsonPath("$.topSellingProducts[1].quantitySold").value(1));
    }

    @Test
    @DisplayName("CA net : un retour supplementaire reduit le CA net du jour")
    void dashboard_netRevenue_reflectsAdditionalReturn() throws Exception {
        // Retour supplementaire de 2x P1 (deja retourne 1 sur 2 vendus dans saleA -> reste 1 retournable)
        // On retourne plutot via une autre vente : 1x de plus depuis saleA n'est plus possible.
        // Retour de 1x P1 encore depuis saleA (vendu 2, deja retourne 1 -> 1 restant).
        returnService.createReturn(new ReturnRequest(saleAId, p1Id, 1, "Second retour"));

        // CA net = 280 - (2 x 50) = 180
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayReturnsValue").value(100.00))
                .andExpect(jsonPath("$.todayNetRevenue").value(180.00));
    }

    @Test
    @DisplayName("Historique des ventes filtre par periode")
    void salesHistory_filteredByPeriod() throws Exception {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(get("/api/admin/sales").param("from", today).param("to", today))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(get("/api/admin/sales").param("from", tomorrow).param("to", tomorrow))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("Historique des ventes filtre par vendeur")
    void salesHistory_filteredBySeller() throws Exception {
        // Cree un second vendeur et une vente a son nom.
        User seller2 = userRepository.save(User.builder()
                .fullName("Vendeur 2").email("seller2@smartboutique.com")
                .password(passwordEncoder.encode("x")).role(Role.VENDEUR).active(true).build());
        sale(seller2.getId(), List.of(new SaleItemRequest(p1Id, 1)));

        mockMvc.perform(get("/api/admin/sales").param("sellerId", adminId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2)); // saleA + saleB

        mockMvc.perform(get("/api/admin/sales").param("sellerId", seller2.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].sellerName").value("Vendeur 2"))
                .andExpect(jsonPath("$.content[0].itemCount").value(1));
    }

    @Test
    @DisplayName("Detail d'une vente : lignes, prix captures, total")
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
                .andExpect(jsonPath("$.content[0].productReference").value("P1"))
                .andExpect(jsonPath("$.content[0].quantity").value(1));
    }

    @Test
    @DisplayName("GET /api/sales/mine : ne renvoie que les ventes de l'utilisateur connecté")
    void myHistory_returnsOnlyCallerSales() throws Exception {
        // Une vente par un autre vendeur ne doit pas apparaître dans « mes ventes ».
        User seller2 = userRepository.save(User.builder()
                .fullName("Vendeur 2").email("seller2@smartboutique.com")
                .password(passwordEncoder.encode("x")).role(Role.VENDEUR).active(true).build());
        sale(seller2.getId(), List.of(new SaleItemRequest(p1Id, 1)));

        // Appelé en tant qu'admin (principal de la classe) -> uniquement saleA + saleB.
        mockMvc.perform(get("/api/sales/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].sellerName").value("Administrateur"));
    }

    @Test
    @WithMockUser(roles = "VENDEUR")
    @DisplayName("Acces au tableau de bord refuse a un VENDEUR : 403")
    void dashboard_forbiddenForSeller() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }
}
