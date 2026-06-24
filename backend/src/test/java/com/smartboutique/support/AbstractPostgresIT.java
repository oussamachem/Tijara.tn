package com.smartboutique.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.TimeZone;

/**
 * Base des tests d'integration : tous tournent contre un PostgreSQL REEL (jamais H2).
 *
 * <p><b>Mode par defaut (CI / standard) :</b> Testcontainers demarre un conteneur
 * PostgreSQL ephemere (singleton, reutilise par toute la suite, nettoye par Ryuk).</p>
 *
 * <p><b>Trappe d'environnement contraint :</b> si {@code SB_TEST_DB_URL} est defini, la suite
 * se connecte a ce PostgreSQL externe plutot que de demarrer un conteneur. Utile quand le
 * client Docker embarque par Testcontainers ne peut pas dialoguer avec le daemon local
 * (ex. incompatibilite de version d'API docker-java <-> Docker Desktop recent). Dans les deux
 * cas, c'est un PostgreSQL reel : l'objectif "plus jamais H2 pour decider du SQL" est tenu.</p>
 *
 * <p>Le fuseau est force a Africa/Tunis (frontiere de journee metier).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractPostgresIT {

    private static final String EXTERNAL_URL = System.getenv("SB_TEST_DB_URL");
    static final PostgreSQLContainer<?> POSTGRES;

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Africa/Tunis"));
        if (EXTERNAL_URL == null) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        if (EXTERNAL_URL == null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_URL);
            registry.add("spring.datasource.username", () -> System.getenv("SB_TEST_DB_USER"));
            registry.add("spring.datasource.password", () -> System.getenv("SB_TEST_DB_PASSWORD"));
        }
    }
}
