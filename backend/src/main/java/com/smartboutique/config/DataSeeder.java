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

    @Value("${app.seed.admin-email}")
    private String adminEmail;

    @Value("${app.seed.admin-password}")
    private String adminPassword;

    @Value("${app.seed.admin-name}")
    private String adminName;

    /** Donnees de demonstration (categories d'exemple) : desactivees en production. */
    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Homme", "Femme", "Enfant", "Chaussures", "Accessoires");

    @Override
    public void run(String... args) {
        // L'admin bootstrap est toujours cree (necessaire pour se connecter, idempotent).
        seedAdmin();
        // Les categories d'exemple ne sont seedees qu'en dehors de la production.
        if (seedDemoData) {
            seedCategories();
        } else {
            log.info("Seed des donnees de demo desactive (app.seed-demo-data=false).");
        }
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
                .build();
        userRepository.save(admin);

        log.info("=================================================================");
        log.info(" Compte administrateur par defaut cree :");
        log.info("   Email        : {}", adminEmail);
        log.info("   Mot de passe : {}", adminPassword);
        log.info("   (a modifier des la premiere connexion)");
        log.info("=================================================================");
    }

    private void seedCategories() {
        DEFAULT_CATEGORIES.forEach(name -> {
            if (!categoryRepository.existsByName(name)) {
                categoryRepository.save(Category.builder().name(name).sizeType(sizeTypeFor(name)).build());
                log.info("Categorie d'exemple creee : {} ({})", name, sizeTypeFor(name));
            }
        });
    }

    private com.smartboutique.entity.SizeType sizeTypeFor(String categoryName) {
        return switch (categoryName) {
            case "Chaussures" -> com.smartboutique.entity.SizeType.SHOE_EU;
            case "Accessoires" -> com.smartboutique.entity.SizeType.NONE;
            default -> com.smartboutique.entity.SizeType.LETTER;
        };
    }
}
