package com.smartboutique.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.TimeZone;

/**
 * Base des tests d'ISOLATION multi-tenant : contrairement a {@link AbstractPostgresIT} (create-drop,
 * sans RLS), ici on demarre le contexte comme en PROD :
 *   - base dediee {@code sbrls} (fraiche, recreee au chargement de la classe) ;
 *   - Flyway applique V1..Vn en tant que PROPRIETAIRE (cree le role sb_app, les policies RLS) ;
 *   - l'app se connecte via le role NON-SUPERUSER {@code sb_app} -> la RLS s'applique reellement ;
 *   - ddl-auto=validate ; seed de demo desactive (INSERT sans tenant violerait NOT NULL).
 *
 * <p>La base est creee dans un bloc statique (avant l'initialisation du contexte Spring et de
 * {@link DynamicPropertySource}). Connexion Postgres = compose via {@code SB_TEST_DB_URL}, sinon
 * conteneur Testcontainers.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class AbstractTenantRlsIT {

    // Mot de passe du role runtime sb_app. sb_app est GLOBAL au cluster : s'il existe deja (compose
    // local), Flyway V9 ne le recree pas et son mot de passe reste celui de la prod -> on le fournit
    // via APP_DB_PASSWORD. En CI (conteneur neuf), sb_app n'existe pas -> V9 le cree avec cette valeur.
    protected static final String APP_PWD =
            System.getenv("APP_DB_PASSWORD") != null ? System.getenv("APP_DB_PASSWORD") : "sb_app_test_pwd";
    protected static String serverUrl;   // jdbc:postgresql://host:port (sans base)
    protected static String rlsUrl;      // .../sbrls
    protected static String ownerUser;
    protected static String ownerPass;
    private static PostgreSQLContainer<?> container;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Tunis"));
        String env = System.getenv("SB_TEST_DB_URL");
        String fullUrl;
        if (env == null) {
            container = new PostgreSQLContainer<>("postgres:16");
            container.start();
            fullUrl = container.getJdbcUrl();
            ownerUser = container.getUsername();
            ownerPass = container.getPassword();
        } else {
            fullUrl = env;
            ownerUser = System.getenv("SB_TEST_DB_USER");
            ownerPass = System.getenv("SB_TEST_DB_PASSWORD");
        }
        // Retire les eventuels parametres puis la base pour obtenir l'URL serveur.
        String noParams = fullUrl.contains("?") ? fullUrl.substring(0, fullUrl.indexOf('?')) : fullUrl;
        serverUrl = noParams.substring(0, noParams.lastIndexOf('/'));
        rlsUrl = serverUrl + "/sbrls";

        try (Connection c = DriverManager.getConnection(serverUrl + "/postgres", ownerUser, ownerPass);
             Statement st = c.createStatement()) {
            st.execute("DROP DATABASE IF EXISTS sbrls WITH (FORCE)");
            st.execute("CREATE DATABASE sbrls");
            // NB : on ne DROP PAS le role sb_app (global, potentiellement utilise par la prod locale) ;
            // Flyway V9 le cree seulement s'il n'existe pas (sinon reutilise l'existant).
        } catch (SQLException e) {
            throw new IllegalStateException("Preparation base d'isolation RLS impossible", e);
        }
    }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        // Runtime : role applicatif NON-SUPERUSER (la RLS mord).
        r.add("spring.datasource.url", () -> rlsUrl);
        r.add("spring.datasource.username", () -> "sb_app");
        r.add("spring.datasource.password", () -> APP_PWD);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        // Flyway en PROPRIETAIRE (DDL / role / policies) + mot de passe sb_app injecte.
        r.add("spring.flyway.enabled", () -> "true");
        r.add("spring.flyway.user", () -> ownerUser);
        r.add("spring.flyway.password", () -> ownerPass);
        r.add("spring.flyway.placeholders.app_role_password", () -> APP_PWD);
        // Pas de seed de demo (categories) : un INSERT sans tenant violerait NOT NULL boutique_id.
        r.add("app.seed-demo-data", () -> "false");
    }

    /** Connexion PROPRIETAIRE (superuser) a sbrls : bypasse la RLS (seed de donnees multi-tenant). */
    protected static Connection ownerConnection() throws SQLException {
        return DriverManager.getConnection(rlsUrl, ownerUser, ownerPass);
    }

    /** Connexion via le role applicatif sb_app (NON-superuser) : la RLS s'applique. */
    protected static Connection appConnection() throws SQLException {
        return DriverManager.getConnection(rlsUrl, "sb_app", APP_PWD);
    }
}
