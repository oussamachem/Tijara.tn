package com.smartboutique.config;

import com.smartboutique.entity.Category;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.entity.ShopMemberRole;
import com.smartboutique.entity.User;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ShopMemberRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Donnees de demarrage (idempotent). Phase A : les comptes sont des IDENTITES ; le role est
 * contextuel (shop_members). Le SUPER_ADMIN = flag is_platform_admin. L'admin bootstrap devient
 * OWNER de la boutique par defaut.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BoutiqueRepository boutiqueRepository;
    private final ShopMemberRepository shopMemberRepository;
    private final CategoryRepository categoryRepository;
    private final PasswordEncoder passwordEncoder;

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

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    private static final List<String> DEFAULT_CATEGORIES =
            List.of("Homme", "Femme", "Enfant", "Chaussures", "Accessoires");

    @Override
    public void run(String... args) {
        seedSuperAdmin();
        seedAdmin();
        if (seedDemoData) {
            seedCategories();
        } else {
            log.info("Seed des donnees de demo desactive (app.seed-demo-data=false).");
        }
    }

    /** Admin plateforme (ex-SUPER_ADMIN) : identite + flag is_platform_admin, aucune boutique. */
    private void seedSuperAdmin() {
        if (userRepository.existsByEmail(superAdminEmail)) return;
        userRepository.save(User.builder()
                .fullName("Super Administrateur")
                .email(superAdminEmail)
                .password(passwordEncoder.encode(superAdminPassword))
                .active(true)
                .platformAdmin(true)
                .build());
        log.info("Compte ADMIN PLATEFORME cree : {}", superAdminEmail);
    }

    /** Admin bootstrap : identite + OWNER de la boutique par defaut (si presente). */
    private void seedAdmin() {
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin == null) {
            admin = userRepository.save(User.builder()
                    .fullName(adminName)
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .active(true)
                    .platformAdmin(false)
                    .build());
            log.info("=================================================================");
            log.info(" Compte proprietaire par defaut cree : {} / {}", adminEmail, adminPassword);
            log.info("=================================================================");
        }
        // Bootstrap : la boutique par defaut (creee par Flyway V9) est possedee par l'admin.
        final Long adminId = admin.getId();
        boutiqueRepository.findBySlug("default").ifPresent(shop -> {
            if (shop.getOwnerUserId() == null) {
                shop.setOwnerUserId(adminId);
                boutiqueRepository.save(shop);
            }
            if (!shopMemberRepository.existsByShopIdAndUserId(shop.getId(), adminId)) {
                shopMemberRepository.save(ShopMember.builder()
                        .shopId(shop.getId()).userId(adminId).role(ShopMemberRole.OWNER).build());
                log.info("Membership OWNER cree : {} -> boutique par defaut", adminEmail);
            }
        });
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
