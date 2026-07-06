package com.smartboutique.tenancy;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PHASE 0 — Spike RLS (gate 0). Prouve, sur une table jouet {@code demo}, que l'isolation
 * multi-tenant par Row Level Security tient AVANT de la generaliser aux tables metier.
 *
 * <p>Test JDBC brut (pas de Spring) : les policies RLS vivent dans du SQL, pas dans le mapping
 * des entites, donc l'infra de test applicative (ddl-auto=create-drop, Flyway off) ne peut pas
 * les exercer. On se connecte a un PostgreSQL reel (compose via {@code SB_TEST_DB_URL}, sinon
 * conteneur Testcontainers) et on prouve l'isolation en tant que ROLE NON-SUPERUSER.</p>
 */
class RlsSpikeIT {

    private static String url, suUser, suPass;
    private static PostgreSQLContainer<?> container;

    private static final String APP_ROLE = "sb_rls_spike";
    private static final String APP_PWD = "spike_pwd";

    @BeforeAll
    static void init() throws SQLException {
        String env = System.getenv("SB_TEST_DB_URL");
        if (env == null) {
            container = new PostgreSQLContainer<>("postgres:16");
            container.start();
            url = container.getJdbcUrl();
            suUser = container.getUsername();
            suPass = container.getPassword();
        } else {
            url = env;
            suUser = System.getenv("SB_TEST_DB_USER");
            suPass = System.getenv("SB_TEST_DB_PASSWORD");
        }
        // Setup en tant que SUPERUSER / proprietaire.
        try (Connection su = su(); Statement st = su.createStatement()) {
            st.execute("DROP TABLE IF EXISTS demo");
            st.execute("DROP ROLE IF EXISTS " + APP_ROLE);
            st.execute("CREATE TABLE demo (id BIGSERIAL PRIMARY KEY, boutique_id BIGINT NOT NULL, val TEXT)");
            st.execute("ALTER TABLE demo ENABLE ROW LEVEL SECURITY");
            st.execute("ALTER TABLE demo FORCE ROW LEVEL SECURITY");   // s'applique aussi au proprietaire
            // NULLIF(..,'') : un GUC custom RESET revient a '' (pas NULL) -> ''::bigint explose.
            // NULLIF ramene unset ET reset a NULL -> comparaison NULL -> 0 ligne (fail-closed).
            st.execute("CREATE POLICY tenant_isolation ON demo "
                    + "USING (boutique_id = NULLIF(current_setting('app.current_boutique', true), '')::bigint) "
                    + "WITH CHECK (boutique_id = NULLIF(current_setting('app.current_boutique', true), '')::bigint)");
            st.execute("CREATE ROLE " + APP_ROLE + " LOGIN PASSWORD '" + APP_PWD + "' NOSUPERUSER");
            st.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON demo TO " + APP_ROLE);
            st.execute("GRANT USAGE, SELECT ON SEQUENCE demo_id_seq TO " + APP_ROLE);
            st.execute("INSERT INTO demo (boutique_id, val) VALUES (1,'A-1'),(1,'A-2'),(2,'B-1')");
        }
    }

    @AfterAll
    static void cleanup() throws SQLException {
        if (url != null) {
            try (Connection su = su(); Statement st = su.createStatement()) {
                st.execute("DROP TABLE IF EXISTS demo");
                st.execute("DROP ROLE IF EXISTS " + APP_ROLE);
            }
        }
        if (container != null) container.stop();
    }

    @Test
    @DisplayName("RLS isole les tenants : lecture, ecriture, WITH CHECK, fail-closed, bypass superuser")
    void rlsIsolatesTenants() throws SQLException {
        try (Connection app = DriverManager.getConnection(url, APP_ROLE, APP_PWD)) {

            // 1) Lecture scopee : tenant 1 ne voit QUE ses 2 lignes.
            exec(app, "SET app.current_boutique = '1'");
            assertThat(count(app)).as("tenant 1 voit ses lignes").isEqualTo(2);

            // 2) tenant 2 ne voit QUE sa ligne.
            exec(app, "SET app.current_boutique = '2'");
            assertThat(count(app)).as("tenant 2 voit ses lignes").isEqualTo(1);

            // 3) Isolation en ECRITURE : tenant 1 ne peut pas modifier une ligne de 2 (invisible -> 0 MAJ).
            exec(app, "SET app.current_boutique = '1'");
            assertThat(update(app, "UPDATE demo SET val='hack' WHERE boutique_id=2"))
                    .as("MAJ cross-tenant = 0 ligne").isEqualTo(0);

            // 4) Insert de SON tenant : OK.
            assertThat(update(app, "INSERT INTO demo (boutique_id, val) VALUES (1,'A-3')")).isEqualTo(1);
            assertThat(count(app)).as("tenant 1 apres son insert").isEqualTo(3);

            // 5) WITH CHECK : tenant 1 ne peut pas INSERER chez le tenant 2 -> erreur.
            assertThatThrownBy(() -> update(app, "INSERT INTO demo (boutique_id, val) VALUES (2,'evil')"))
                    .isInstanceOf(SQLException.class);

            // 6) Fail-closed : sans GUC, aucune ligne visible.
            exec(app, "RESET app.current_boutique");
            assertThat(count(app)).as("sans tenant = fail-closed").isEqualTo(0);
        }

        // 7) Bypass SUPER_ADMIN : un superuser (role plateforme) voit TOUT, RLS ignoree.
        try (Connection su = su()) {
            assertThat(count(su)).as("superuser voit tous les tenants").isEqualTo(4); // 3 (t1) + 1 (t2)
            assertThat(scalarInt(su, "SELECT count(*) FROM demo WHERE val='hack'"))
                    .as("aucune ligne n'a ete piratee").isEqualTo(0);
        }
    }

    // --------------------------- helpers ---------------------------

    private static Connection su() throws SQLException {
        return DriverManager.getConnection(url, suUser, suPass);
    }

    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) { st.execute(sql); }
    }

    private static int update(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement()) { return st.executeUpdate(sql); }
    }

    private static int count(Connection c) throws SQLException {
        return scalarInt(c, "SELECT count(*) FROM demo");
    }

    private static int scalarInt(Connection c, String sql) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
