package com.smartboutique.service;

import com.smartboutique.dto.BoutiqueResponse;
import com.smartboutique.dto.CreateBoutiqueRequest;
import com.smartboutique.dto.GoodexSettingsRequest;
import com.smartboutique.dto.GoodexSettingsResponse;
import com.smartboutique.dto.ShopResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.Color;
import com.smartboutique.entity.ShopMember;
import com.smartboutique.entity.ShopMemberRole;
import com.smartboutique.entity.Size;
import com.smartboutique.entity.User;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ColorRepository;
import com.smartboutique.repository.ShopMemberRepository;
import com.smartboutique.repository.SizeRepository;
import com.smartboutique.repository.UserRepository;
import com.smartboutique.service.storage.ImageStorage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ImageStorage fileStorageService;

    @PersistenceContext
    private EntityManager entityManager;

    // ---- Catalogue par défaut d'une NOUVELLE boutique (couleurs + tailles), modifiable ensuite. ----
    private record DefaultColor(String name, String hex) {}
    private static final List<DefaultColor> DEFAULT_COLORS = List.of(
            new DefaultColor("Noir", "#111111"),
            new DefaultColor("Blanc", "#FFFFFF"),
            new DefaultColor("Gris", "#9CA3AF"),
            new DefaultColor("Rouge", "#DC2626"),
            new DefaultColor("Bleu", "#2563EB"),
            new DefaultColor("Vert", "#16A34A"),
            new DefaultColor("Jaune", "#FACC15"),
            new DefaultColor("Rose", "#EC4899"),
            new DefaultColor("Beige", "#D9C6A5"),
            new DefaultColor("Marron", "#92400E"));
    private static final List<String> DEFAULT_SIZES = List.of(
            "S", "M", "L", "XL", "XXL", "XXXL",
            "36", "37", "38", "39", "40", "41", "42", "43", "44", "45", "46", "47", "48", "49");

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

    // ------------------------------- Contact WhatsApp (public) -------------------------------

    /** Réglages WhatsApp : numéro (normalisé au format international) + message par défaut (vide -> null). */
    @Transactional
    public ShopResponse updateContact(Long id, String rawPhone, String defaultMessage) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id));
        b.setContactPhone(normalizeWhatsapp(rawPhone));
        b.setWhatsappDefaultMessage(blankToNull(defaultMessage));
        boutiqueRepository.save(b);
        return ShopResponse.of(b);
    }

    /**
     * Normalise un numéro au format wa.me : {@code +<indicatif><numéro>} (chiffres uniquement).
     * Vide/blanc -> {@code null} (efface). Format invalide (hors 8..15 chiffres) -> 400.
     * Accepte « +216 12 345 678 », « 0021612345678 », « 21612345678 » -> « +21612345678 ».
     */
    static String normalizeWhatsapp(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.startsWith("00")) digits = digits.substring(2);   // préfixe d'appel international
        if (digits.length() < 8 || digits.length() > 15) {
            throw new BusinessException(
                    "Numéro WhatsApp invalide : utilisez le format international, ex. +21612345678.",
                    HttpStatus.BAD_REQUEST);
        }
        return "+" + digits;
    }

    // ------------------------------- Réglages Goodex (transporteur) -------------------------------

    /** Réglages Goodex de la boutique (token jamais exposé -> booléen {@code configured}). */
    @Transactional(readOnly = true)
    public GoodexSettingsResponse getGoodexSettings(Long id) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id));
        return new GoodexSettingsResponse(b.getGoodexUserId(), b.getGoodexBaseUrl(),
                b.getGoodexToken() != null && !b.getGoodexToken().isBlank());
    }

    /** Met à jour les identifiants Goodex. Token vide -> inchangé (on n'écrase pas un token existant). */
    @Transactional
    public GoodexSettingsResponse updateGoodexSettings(Long id, GoodexSettingsRequest req) {
        Boutique b = boutiqueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", id));
        if (req.token() != null && !req.token().isBlank()) b.setGoodexToken(req.token().trim());
        b.setGoodexUserId(blankToNull(req.userId()));
        b.setGoodexBaseUrl(blankToNull(req.baseUrl()));
        boutiqueRepository.save(b);
        log.info("[Goodex] Réglages mis à jour pour la boutique {} (user_id={})", id, b.getGoodexUserId());
        return new GoodexSettingsResponse(b.getGoodexUserId(), b.getGoodexBaseUrl(),
                b.getGoodexToken() != null && !b.getGoodexToken().isBlank());
    }

    private static String blankToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
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

        // 4) Catalogue de base (couleurs + tailles par defaut, modifiables ensuite).
        seedDefaultCatalog(boutique.getId());

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
        // Catalogue de base (couleurs + tailles par defaut, modifiables ensuite).
        seedDefaultCatalog(boutique.getId());
        log.info("[SELF-SERVICE] Boutique '{}' (slug={}, id={}) creee par l'utilisateur {}",
                boutique.getName(), slug, boutique.getId(), ownerUserId);
        return BoutiqueResponse.of(boutique);
    }

    /**
     * Seed le catalogue de base d'une NOUVELLE boutique : couleurs + tailles par defaut. Doit tourner
     * DANS la transaction de creation (meme connexion) : on positionne le tenant courant sur la
     * nouvelle boutique via {@code app.current_boutique} pour que le DEFAULT SQL {@code boutique_id}
     * ET la RLS (WITH CHECK) resolvent au bon tenant. Le proprietaire pourra tout modifier ensuite.
     */
    private void seedDefaultCatalog(Long boutiqueId) {
        entityManager.flush();   // la boutique (FK boutique_id) doit exister avant les inserts scopes
        // SET LOCAL du tenant courant sur la nouvelle boutique (scope transaction).
        entityManager.createNativeQuery("SELECT set_config('app.current_boutique', :bid, true)")
                .setParameter("bid", String.valueOf(boutiqueId))
                .getSingleResult();
        for (DefaultColor c : DEFAULT_COLORS) {
            colorRepository.save(Color.builder().name(c.name()).hex(c.hex()).build());
        }
        int pos = 1;
        for (String label : DEFAULT_SIZES) {
            sizeRepository.save(Size.builder().label(label).position(pos++).build());
        }
        entityManager.flush();   // materialise les inserts sous le bon tenant (DEFAULT + RLS)
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
