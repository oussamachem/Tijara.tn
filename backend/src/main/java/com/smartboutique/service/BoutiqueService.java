package com.smartboutique.service;

import com.smartboutique.dto.BoutiqueResponse;
import com.smartboutique.dto.CreateBoutiqueRequest;
import com.smartboutique.dto.ShopResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.entity.ShopMemberRole;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ShopMemberRepository;
import com.smartboutique.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Gestion des boutiques (tenants) par le SUPER_ADMIN : creation d'une boutique + son admin initial,
 * suspension/reactivation, liste. Une boutique suspendue bloque la connexion de ses utilisateurs
 * (cf. {@code CustomUserDetailsService}) et disparaitra de l'annuaire client (Phase 4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoutiqueService {

    private final BoutiqueRepository boutiqueRepository;
    private final UserRepository userRepository;
    private final ShopMemberRepository shopMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    /** Fiche de la boutique (pour son proprietaire) : nom, slug, logo. */
    @Transactional(readOnly = true)
    public ShopResponse getShop(Long id) {
        return ShopResponse.of(boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id)));
    }

    /** Met a jour le logo de la boutique (remplace l'ancien fichier). */
    @Transactional
    public ShopResponse updateLogo(Long id, MultipartFile file) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id));
        String old = b.getLogoUrl();
        b.setLogoUrl(fileStorageService.store(file));
        boutiqueRepository.save(b);
        if (old != null) fileStorageService.delete(old);
        return ShopResponse.of(b);
    }

    /** Retire le logo de la boutique. */
    @Transactional
    public ShopResponse removeLogo(Long id) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id));
        String old = b.getLogoUrl();
        b.setLogoUrl(null);
        boutiqueRepository.save(b);
        if (old != null) fileStorageService.delete(old);
        return ShopResponse.of(b);
    }

    @Transactional
    public BoutiqueResponse create(CreateBoutiqueRequest request) {
        String email = request.adminEmail().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                    "Un utilisateur existe deja avec l'email " + email, HttpStatus.CONFLICT);
        }
        String slug = uniqueSlug(request.slug() != null && !request.slug().isBlank()
                ? slugify(request.slug()) : slugify(request.name()));

        // 1) Le compte OWNER (identite globale) est cree d'abord.
        String adminName = (request.adminName() != null && !request.adminName().isBlank())
                ? request.adminName().trim() : "Proprietaire " + request.name().trim();
        User owner = userRepository.save(User.builder()
                .fullName(adminName)
                .email(email)
                .password(passwordEncoder.encode(request.adminPassword()))
                .active(true)
                .platformAdmin(false)
                .build());

        // 2) La boutique, rattachee a son proprietaire.
        Boutique boutique = boutiqueRepository.save(Boutique.builder()
                .name(request.name().trim())
                .slug(slug)
                .status(BoutiqueStatus.ACTIVE)
                .ownerUserId(owner.getId())
                .build());

        // 3) Le membership OWNER (role contextuel).
        shopMemberRepository.save(ShopMember.builder()
                .shopId(boutique.getId()).userId(owner.getId()).role(ShopMemberRole.OWNER).build());

        log.info("[PLATEFORME] Boutique '{}' (slug={}, id={}) creee, proprietaire {}",
                boutique.getName(), slug, boutique.getId(), email);
        return BoutiqueResponse.of(boutique);
    }

    /**
     * Self-service (Phase B) : l'utilisateur AUTHENTIFIE (identite existante) cree SA boutique et en
     * devient OWNER. Aucun nouveau compte n'est cree (contrairement a {@link #create}). Le slug est
     * derive du nom et rendu unique.
     */
    @Transactional
    public BoutiqueResponse createForOwner(String name, Long ownerUserId) {
        String slug = uniqueSlug(slugify(name));
        Boutique boutique = boutiqueRepository.save(Boutique.builder()
                .name(name.trim())
                .slug(slug)
                .status(BoutiqueStatus.ACTIVE)
                .ownerUserId(ownerUserId)
                .build());
        shopMemberRepository.save(ShopMember.builder()
                .shopId(boutique.getId()).userId(ownerUserId).role(ShopMemberRole.OWNER).build());
        log.info("[SELF-SERVICE] Boutique '{}' (slug={}, id={}) creee par l'utilisateur {}",
                boutique.getName(), slug, boutique.getId(), ownerUserId);
        return BoutiqueResponse.of(boutique);
    }

    @Transactional
    public BoutiqueResponse suspend(Long id) {
        return setStatus(id, BoutiqueStatus.SUSPENDED);
    }

    @Transactional
    public BoutiqueResponse reactivate(Long id) {
        return setStatus(id, BoutiqueStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public List<BoutiqueResponse> list() {
        return boutiqueRepository.findAll().stream().map(BoutiqueResponse::of).toList();
    }

    private BoutiqueResponse setStatus(Long id, BoutiqueStatus status) {
        Boutique boutique = boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id));
        boutique.setStatus(status);
        log.warn("[PLATEFORME] Boutique '{}' (id={}) -> {}", boutique.getName(), id, status);
        return BoutiqueResponse.of(boutiqueRepository.save(boutique));
    }

    // --------------------------- slug ---------------------------

    private String slugify(String input) {
        String noAccents = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String slug = noAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isBlank() ? "boutique" : slug;
    }

    /** Garantit l'unicite du slug en suffixant -2, -3, ... si necessaire. */
    private String uniqueSlug(String base) {
        String candidate = base;
        int n = 2;
        while (boutiqueRepository.existsBySlug(candidate)) {
            candidate = base + "-" + n++;
        }
        return candidate;
    }
}
