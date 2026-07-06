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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GATE 3 — le joyau : isolation multi-tenant prouvee de bout en bout sur PostgreSQL reel avec RLS,
 * a travers l'API (vrais JWT -> le filtre pose le tenant -> l'aspect pose le GUC -> RLS filtre).
 * Un utilisateur de la boutique A ne lit ni n'ecrit JAMAIS la donnee de la boutique B.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TenantIsolationIT extends AbstractTenantRlsIT {

    @Autowired private MockMvc mockMvc;

    private String saToken;           // SUPER_ADMIN
    private long idA, idB;            // boutiques
    private String tokenA, tokenB;    // admins de A et B
    private long prodA, prodB, variantB;

    @BeforeAll
    void setUp() throws Exception {
        saToken = login("superadmin@smartboutique.com", "Super@123");

        idA = createBoutique("Alpha", "iso.admin.a@shop.test");
        idB = createBoutique("Beta", "iso.admin.b@shop.test");
        tokenA = login("iso.admin.a@shop.test", "Passw0rd!");
        tokenB = login("iso.admin.b@shop.test", "Passw0rd!");

        // Donnees semees en tant que PROPRIETAIRE (bypass RLS), boutique_id explicite.
        try (Connection c = ownerConnection()) {
            long[] a = seedProduct(c, idA, "A");
            long[] b = seedProduct(c, idB, "B");
            prodA = a[0];
            prodB = b[0];
            variantB = b[1];
        }
    }

    // -------------------------------- Lecture --------------------------------

    @Test
    @DisplayName("A ne voit QUE ses produits ; le produit de B est invisible (liste + acces direct)")
    void readIsolation() throws Exception {
        mockMvc.perform(get("/api/products?size=50").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].reference").value("A-P"));

        // Acces direct au produit de B par son id -> 404 (RLS le masque).
        mockMvc.perform(get("/api/products/{id}", prodB).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
        // A lit bien le sien.
        mockMvc.perform(get("/api/products/{id}", prodA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Le tableau de bord de A differe de celui de B (chacun ne compte que ses donnees)")
    void dashboardIsolation() throws Exception {
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalProducts").value(1));
        mockMvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalProducts").value(1));
    }

    // -------------------------------- Ecriture --------------------------------

    @Test
    @DisplayName("A ne peut pas vendre une variante de B (invisible -> refuse)")
    void writeIsolation_cannotSellForeignVariant() throws Exception {
        String body = "{\"items\":[{\"variantId\":" + variantB + ",\"quantity\":1}],\"paymentMethod\":\"ESPECES\"}";
        mockMvc.perform(post("/api/sales").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is4xxClientError());   // 404 : la variante de B n'existe pas pour A
        // La variante de B est intacte (stock inchange).
        assertThat(variantStock(variantB)).isEqualTo(10);
    }

    @Test
    @DisplayName("Vendeurs (table users, sans RLS) : scoping applicatif -> A ne voit pas le vendeur de B")
    void sellerIsolation_appLayerScoping() throws Exception {
        // A cree un vendeur.
        String seller = "{\"fullName\":\"Vendeur A\",\"email\":\"seller.a@shop.test\",\"password\":\"Passw0rd!\"}";
        String json = mockMvc.perform(post("/api/admin/sellers").header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON).content(seller))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long sellerAId = ((Number) JsonPath.read(json, "$.id")).longValue();

        // A voit son vendeur ; B ne le voit pas (liste + acces direct).
        mockMvc.perform(get("/api/admin/sellers").header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$[?(@.email=='seller.a@shop.test')]").exists());
        mockMvc.perform(get("/api/admin/sellers").header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$[?(@.email=='seller.a@shop.test')]").doesNotExist());
        mockMvc.perform(get("/api/admin/sellers/{id}", sellerAId).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    // ---------------------------- Preuve BD directe ----------------------------

    @Test
    @DisplayName("En base (role sb_app) : sous le tenant A, aucune ligne de B ; sans tenant, rien")
    void dbLevelIsolation() throws Exception {
        try (Connection c = appConnection(); Statement st = c.createStatement()) {
            st.execute("SET app.current_boutique = '" + idA + "'");
            assertThat(scalar(st, "SELECT count(*) FROM products")).isEqualTo(1);
            assertThat(scalar(st, "SELECT count(*) FROM products WHERE boutique_id = " + idB)).isZero();

            st.execute("SET app.current_boutique = '" + idB + "'");
            assertThat(scalar(st, "SELECT count(*) FROM products")).isEqualTo(1);

            st.execute("RESET app.current_boutique");
            assertThat(scalar(st, "SELECT count(*) FROM products")).as("fail-closed sans tenant").isZero();
        }
    }

    // -------------------------------- helpers --------------------------------

    private String login(String email, String pwd) throws Exception {
        String body = "{\"email\":\"" + email + "\",\"password\":\"" + pwd + "\"}";
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.token");
    }

    private long createBoutique(String name, String adminEmail) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"adminEmail\":\"" + adminEmail
                + "\",\"adminPassword\":\"Passw0rd!\"}";
        String json = mockMvc.perform(post("/api/admin/boutiques").header("Authorization", "Bearer " + saToken)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    /** Seme categorie+couleur+taille+produit+variante pour un tenant (owner, bypass RLS). Renvoie [productId, variantId]. */
    private long[] seedProduct(Connection c, long boutiqueId, String p) throws Exception {
        long cat = insert(c, "INSERT INTO categories(name, boutique_id) VALUES ('" + p + "-Cat', " + boutiqueId + ") RETURNING id");
        long col = insert(c, "INSERT INTO colors(name, boutique_id) VALUES ('" + p + "-Bleu', " + boutiqueId + ") RETURNING id");
        long siz = insert(c, "INSERT INTO sizes(label, boutique_id) VALUES ('" + p + "-M', " + boutiqueId + ") RETURNING id");
        long prod = insert(c, "INSERT INTO products(reference, name, category_id, purchase_price, sale_price, created_at, boutique_id) "
                + "VALUES ('" + p + "-P', 'Produit " + p + "', " + cat + ", 5, 20, now(), " + boutiqueId + ") RETURNING id");
        long var = insert(c, "INSERT INTO product_variant(product_id, color_id, size_id, quantity, seuil_alerte, reference, qr_code, created_at, boutique_id) "
                + "VALUES (" + prod + ", " + col + ", " + siz + ", 10, 0, '" + p + "-P-M-BLEU', '" + p + "-P-M-BLEU', now(), " + boutiqueId + ") RETURNING id");
        return new long[]{prod, var};
    }

    private long insert(Connection c, String sql) throws Exception {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private int variantStock(long variantId) throws Exception {
        try (Connection c = ownerConnection(); Statement st = c.createStatement()) {
            return scalar(st, "SELECT quantity FROM product_variant WHERE id = " + variantId);
        }
    }

    private int scalar(Statement st, String sql) throws Exception {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
