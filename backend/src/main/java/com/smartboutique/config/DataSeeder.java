package com.smartboutique.config;

import com.smartboutique.entity.Category;
import com.smartboutique.entity.Role;
import com.smartboutique.entity.User;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Initialise les donnees de demarrage au premier lancement :
 *  - un compte administrateur par defaut ;
 *  - quelques categories d'exemple.
 * Idempotent : ne cree que ce qui n'existe pas encore.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.admin-name}")
    private String adminName;

    @Value("${app.seed.super-admin-email}")
    private String superAdminEmail;

    @Value("${app.seed.super-admin-password}")
    private String superAdminPassword;

    /** Donnees de demonstration (categories d'exemple) : desactivees en production. */
    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Homme", "Femme", "Enfant", "Chaussures", "Accessoires");

    @Override
    public void run(String... args) {
        // Le SUPER_ADMIN plateforme + l'admin bootstrap sont toujours crees (idempotent).
        seedSuperAdmin();
        seedAdmin();
        // Les categories d'exemple ne sont seedees qu'en dehors de la production.
        if (seedDemoData) {
            seedCategories();
        } else {
            log.info("Seed des donnees de demo desactive (app.seed-demo-data=false).");
        }
    }

    private void seedSuperAdmin() {
        if (userRepository.existsByEmail(superAdminEmail)) {
            return;
        }
        userRepository.save(User.builder()
                .fullName("Super Administrateur")
                .email(superAdminEmail)
                .password(passwordEncoder.encode(superAdminPassword))
                .role(Role.SUPER_ADMIN)
                .active(true)
                .boutiqueId(defaultBoutiqueId())   // rattache a la boutique par defaut (NOT NULL en prod)
                .build());
        log.info("Compte SUPER_ADMIN plateforme cree : {}", superAdminEmail);
    }

    private void seedAdmin() {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Compte administrateur deja present ({}), seed ignore.", adminEmail);
            return;
        }
        User admin = User.builder()
                .fullName(adminName)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .active(true)
                .boutiqueId(defaultBoutiqueId())   // prod : boutique par defaut (NOT NULL) ; dev/test : null
                .build();
        userRepository.save(admin);

        log.info("=================================================================");
        log.info(" Compte administrateur par defaut cree :");
        log.info("   Email        : {}", adminEmail);
        log.info("   Mot de passe : {}", adminPassword);
        log.info("   (a modifier des la premiere connexion)");
        log.info("=================================================================");
    }

    /**
     * Id de la boutique par defaut (prod : table boutiques presente via Flyway V9). En dev/test
     * (schema create-drop sans la table), renvoie null : la colonne users.boutique_id y est nullable.
     */
    private Long defaultBoutiqueId() {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM boutiques WHERE slug = 'default'", Long.class);
        } catch (DataAccessException e) {
            return null;
        }
    }

    private void seedCategories() {
        DEFAULT_CATEGORIES.forEach(name -> {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(Category.builder().name(name).build());
                log.info("Categorie d'exemple creee : {}", name);
            }
        });
    }
}
