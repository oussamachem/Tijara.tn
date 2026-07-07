package com.smartboutique.tenancy;

import com.jayway.jsonpath.JsonPath;
import com.smartboutique.support.AbstractTenantRlsIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 5 — Gestion des commandes cote boutique : cycle de vie, stock (decrement a la confirmation /
 * restauration a l'annulation, jamais negatif), transitions controlees, isolation multi-tenant.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderManagementIT extends AbstractTenantRlsIT {

    @Autowired private MockMvc mockMvc;

    private String adminToken;      // admin de la boutique "order-shop"
    private String otherAdminToken; // admin d'une autre boutique (isolation)
    private String clientToken;
    private long shopId;
    private final String slug = "order-shop";

    @BeforeAll
    void setUp() throws Exception {
        String sa = login("superadmin@smartboutique.com", "Super@123");
        shopId = createBoutique(sa, "Order Shop", "order.admin@shop.test");
        createBoutique(sa, "Other Shop", "other.admin@shop.test");
        adminToken = login("order.admin@shop.test", "Passw0rd!");
        otherAdminToken = login("other.admin@shop.test", "Passw0rd!");
        clientToken = registerClient("order.client@shop.test");
    }

    // --------------------------------- Stock ---------------------------------

    @Test
    @DisplayName("Confirmation -> stock decremente (variante) ; le detail expose l'historique")
    void confirm_decrementsStock() throws Exception {
        long variant = seedVariant(shopId, "S1", 5);
        long orderId = placeOrder(variant, 2);
        assertThat(stock(variant)).isEqualTo(5);   // pas de decrement a la commande (C3)

        changeStatus(adminToken, orderId, "CONFIRMEE")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMEE"))
                .andExpect(jsonPath("$.history.length()").value(2));   // creation + confirmation
        assertThat(stock(variant)).isEqualTo(3);   // 5 - 2
    }

    @Test
    @DisplayName("Annulation apres confirmation -> stock restaure ; annulation avant -> stock inchange")
    void cancel_restoresStockOnlyIfDecremented() throws Exception {
        long variant = seedVariant(shopId, "S2", 5);
        long confirmed = placeOrder(variant, 2);
        changeStatus(adminToken, confirmed, "CONFIRMEE").andExpect(status().isOk());
        assertThat(stock(variant)).isEqualTo(3);
        changeStatus(adminToken, confirmed, "ANNULEE").andExpect(status().isOk());
        assertThat(stock(variant)).as("stock restaure").isEqualTo(5);

        // Annulation d'une commande NON confirmee : stock inchange.
        long pending = placeOrder(variant, 1);
        changeStatus(adminToken, pending, "ANNULEE").andExpect(status().isOk());
        assertThat(stock(variant)).isEqualTo(5);
    }

    @Test
    @DisplayName("Confirmation impossible si stock insuffisant -> 409, stock inchange (jamais negatif)")
    void confirm_insufficientStock_conflict() throws Exception {
        long variant = seedVariant(shopId, "S3", 1);
        long orderId = placeOrder(variant, 3);   // commande 3 pour un stock de 1
        changeStatus(adminToken, orderId, "CONFIRMEE").andExpect(status().isConflict());
        assertThat(stock(variant)).isEqualTo(1); // inchange, jamais negatif
    }

    // ----------------------------- Transitions -----------------------------

    @Test
    @DisplayName("Transition invalide (EN_ATTENTE -> PRETE) -> 409 ; cycle complet valide")
    void transitions_controlled() throws Exception {
        long variant = seedVariant(shopId, "S4", 10);
        long orderId = placeOrder(variant, 1);

        changeStatus(adminToken, orderId, "PRETE").andExpect(status().isConflict());        // saut interdit
        changeStatus(adminToken, orderId, "CONFIRMEE").andExpect(status().isOk());
        changeStatus(adminToken, orderId, "PRETE").andExpect(status().isOk());
        changeStatus(adminToken, orderId, "RECUPEREE").andExpect(status().isOk());
        changeStatus(adminToken, orderId, "ANNULEE").andExpect(status().isConflict());      // terminal
    }

    // ------------------------------ Isolation ------------------------------

    @Test
    @DisplayName("Isolation : l'admin d'une AUTRE boutique ne voit ni ne modifie la commande")
    void isolation_otherAdminCannotTouch() throws Exception {
        long variant = seedVariant(shopId, "S5", 5);
        long orderId = placeOrder(variant, 1);

        // admin d'order-shop voit sa commande
        mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$[?(@.id==" + orderId + ")]").exists());
        // admin de other-shop ne la voit pas, ni en detail, ni en action
        mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(jsonPath("$[?(@.id==" + orderId + ")]").doesNotExist());
        mockMvc.perform(get("/api/admin/orders/{id}", orderId).header("Authorization", "Bearer " + otherAdminToken))
                .andExpect(status().isNotFound());
        changeStatus(otherAdminToken, orderId, "CONFIRMEE").andExpect(status().isNotFound());
        assertThat(stock(variant)).isEqualTo(5);   // non touche par l'autre boutique
    }

    // -------------------------------- helpers --------------------------------

    private org.springframework.test.web.servlet.ResultActions changeStatus(String token, long id, String s) throws Exception {
        return mockMvc.perform(post("/api/admin/orders/{id}/status", id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"" + s + "\"}"));
    }

    private String login(String email, String pwd) throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + pwd + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private String registerClient(String email) throws Exception {
        String json = mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Client\",\"email\":\"" + email + "\",\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private long createBoutique(String sa, String name, String adminEmail) throws Exception {
        String json = mockMvc.perform(post("/api/admin/boutiques").header("Authorization", "Bearer " + sa)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"adminEmail\":\"" + adminEmail + "\",\"adminPassword\":\"Passw0rd!\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long placeOrder(long variantId, int qty) throws Exception {
        String json = mockMvc.perform(post("/api/shops/{slug}/orders", slug).header("Authorization", "Bearer " + clientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":" + variantId + ",\"quantity\":" + qty + "}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Seme categorie+couleur+taille+produit+variante (owner, bypass RLS) et renvoie l'id variante. */
    private long seedVariant(long boutiqueId, String p, int stock) throws Exception {
        try (Connection c = ownerConnection()) {
            long cat = ins(c, "INSERT INTO categories(name,boutique_id) VALUES ('" + p + "-C'," + boutiqueId + ") RETURNING id");
            long col = ins(c, "INSERT INTO colors(name,boutique_id) VALUES ('" + p + "-Bleu'," + boutiqueId + ") RETURNING id");
            long siz = ins(c, "INSERT INTO sizes(label,boutique_id) VALUES ('" + p + "-M'," + boutiqueId + ") RETURNING id");
            long prod = ins(c, "INSERT INTO products(reference,name,category_id,purchase_price,sale_price,created_at,boutique_id) "
                    + "VALUES ('" + p + "-P','Prod " + p + "'," + cat + ",5,20,now()," + boutiqueId + ") RETURNING id");
            return ins(c, "INSERT INTO product_variant(product_id,color_id,size_id,quantity,seuil_alerte,reference,qr_code,created_at,boutique_id) "
                    + "VALUES (" + prod + "," + col + "," + siz + "," + stock + ",0,'" + p + "-V','" + p + "-V',now()," + boutiqueId + ") RETURNING id");
        }
    }

    private int stock(long variantId) throws Exception {
        try (Connection c = ownerConnection(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT quantity FROM product_variant WHERE id=" + variantId)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private long ins(Connection c, String sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
}
