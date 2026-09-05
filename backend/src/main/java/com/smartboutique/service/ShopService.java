package com.smartboutique.service;

import com.smartboutique.dto.FeedProductResponse;
import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.ProductOgData;
import com.smartboutique.dto.PublicProductResponse;
import com.smartboutique.dto.PublicProductResponse.PublicImage;
import com.smartboutique.dto.PublicProductResponse.PublicVariantResponse;
import com.smartboutique.dto.ShopResponse;
import com.smartboutique.dto.ShopStatsResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.ProductImage;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.repository.SaleRepository;
import com.smartboutique.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Marketplace public. La resolution du tenant se fait par le SLUG (pas par JWT) : {@link #enterShop}
 * pose le {@link TenantContext} a partir de la boutique du slug AVANT toute requete sur des tables
 * tenant (produits, images, commandes) -> l'aspect pose le GUC -> la RLS scope au bon tenant.
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final BoutiqueRepository boutiqueRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final SaleRepository saleRepository;
    private final FollowService followService;

    // Auto-injection (proxy) : le fil marketplace appelle feedForShop() PAR boutique afin que chaque
    // requete ouvre sa PROPRE transaction -> l'aspect pose le tenant (RLS) de cette boutique. Un
    // appel direct (this.) court-circuiterait le proxy -> pas de transaction -> pas de tenant.
    @Autowired
    @Lazy
    private ShopService self;

    /**
     * Fil marketplace (accueil client) : melange de produits PUBLICS de TOUTES les boutiques actives.
     * Chaque boutique est lue dans sa propre transaction (RLS) via {@link #feedForShop}. Champs deja
     * publics (nom, prix, image de couverture) -> pas de fuite cross-tenant.
     */
    public List<FeedProductResponse> feed(int limit) {
        List<Boutique> shops = boutiqueRepository.searchActive("");
        if (shops.isEmpty()) return List.of();
        int perShop = Math.min(15, Math.max(3, (limit / shops.size()) + 3));
        List<FeedProductResponse> out = new ArrayList<>();
        for (Boutique b : shops) {
            TenantContext.set(b.getId());
            try {
                out.addAll(self.feedForShop(b, perShop));
            } catch (RuntimeException ignore) {
                // Une boutique en erreur ne casse pas le fil.
            } finally {
                TenantContext.clear();
            }
        }
        Collections.shuffle(out);   // effet "decouverte" (a defaut de ranking par ventes cross-boutique)
        return out.stream().limit(limit).toList();
    }

    /** Produits disponibles d'UNE boutique (tenant courant deja pose par l'appelant) -> cartes de fil. */
    @Transactional(readOnly = true)
    public List<FeedProductResponse> feedForShop(Boutique b, int limit) {
        Page<Product> products = productRepository.findAll(
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<FeedProductResponse> res = new ArrayList<>();
        for (Product p : products) {
            if (availableVariants(p).isEmpty()) continue;   // seulement le disponible
            String img = p.getImages().stream()
                    .min(Comparator.comparingInt(ProductImage::getPosition))
                    .map(ProductImage::getUrl).orElse(null);
            res.add(new FeedProductResponse(b.getSlug(), b.getName(), b.getLogoUrl(), p.getId(), p.getName(), p.getSalePrice(), img));
        }
        return res;
    }

    // ==================== MARKETPLACE GLOBALE (barre de categories, cross-boutique) ====================

    /** Ordre d'affichage prioritaire des grandes familles ; le reste suit par ordre alphabetique. */
    private static final List<String> CATEGORY_PRIORITY = List.of(
            "femme", "homme", "enfant", "robes", "chaussures", "accessoires");

    /**
     * Noms de categories DISTINCTS presents dans la marketplace (categories ayant au moins un produit
     * disponible), tous marchands confondus. Regroupement par nom insensible a la casse/aux espaces
     * (MVP : pas de referentiel global). Chaque boutique est lue sous sa propre RLS via {@link #self}.
     */
    public List<String> marketplaceCategories() {
        List<Boutique> shops = boutiqueRepository.searchActive("");
        Map<String, String> byKey = new LinkedHashMap<>();   // cle normalisee -> libelle affiche (1er vu)
        for (Boutique b : shops) {
            TenantContext.set(b.getId());
            try {
                for (String name : self.categoryNamesForShop()) {
                    byKey.putIfAbsent(normalizeCategory(name), name.trim());
                }
            } catch (RuntimeException ignore) {
                // une boutique en erreur ne casse pas la barre
            } finally {
                TenantContext.clear();
            }
        }
        return byKey.entrySet().stream()
                .sorted(Comparator
                        .comparingInt((Map.Entry<String, String> e) -> {
                            int i = CATEGORY_PRIORITY.indexOf(e.getKey());
                            return i < 0 ? Integer.MAX_VALUE : i;
                        })
                        .thenComparing(Map.Entry::getValue, String.CASE_INSENSITIVE_ORDER))
                .map(Map.Entry::getValue)
                .toList();
    }

    /** Noms de categories du tenant courant ayant au moins un produit disponible. Via proxy (RLS posee). */
    @Transactional(readOnly = true)
    public Set<String> categoryNamesForShop() {
        Set<String> names = new LinkedHashSet<>();
        for (Product p : productRepository.findAll()) {   // RLS -> produits du tenant courant
            Category c = p.getCategory();
            if (c == null || c.getName() == null || c.getName().isBlank()) continue;
            if (availableVariants(p).isEmpty()) continue;
            names.add(c.getName().trim());
        }
        return names;
    }

    /**
     * Fil marketplace d'UNE categorie (par NOM), cross-boutique. Champs strictement PUBLICS
     * (nom produit, prix de vente, image de couverture, boutique) -> jamais de cout/marge/stock
     * d'une autre boutique. Pagination en memoire sur un ordre deterministe -> scroll infini stable.
     */
    public PageResponse<FeedProductResponse> marketplaceProducts(String category, int page, int size) {
        String norm = normalizeCategory(category);
        List<Boutique> shops = norm.isEmpty() ? List.of() : boutiqueRepository.searchActive("");
        List<FeedProductResponse> all = new ArrayList<>();
        for (Boutique b : shops) {
            TenantContext.set(b.getId());
            try {
                all.addAll(self.categoryProductsForShop(b, norm));
            } catch (RuntimeException ignore) {
                // une boutique en erreur ne casse pas le fil
            } finally {
                TenantContext.clear();
            }
        }
        all.sort(Comparator.comparing(FeedProductResponse::productId).reversed());   // ordre stable inter-pages
        int total = all.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) total / size));
        int from = Math.min(page * size, total);
        int to = Math.min(from + size, total);
        List<FeedProductResponse> content = new ArrayList<>(all.subList(from, to));
        boolean last = page >= totalPages - 1;
        return new PageResponse<>(content, page, size, total, totalPages, last);
    }

    /** Produits disponibles du tenant courant appartenant a la categorie normalisee. Via proxy (RLS). */
    @Transactional(readOnly = true)
    public List<FeedProductResponse> categoryProductsForShop(Boutique b, String norm) {
        List<FeedProductResponse> res = new ArrayList<>();
        for (Product p : productRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))) {
            Category c = p.getCategory();
            if (c == null || !norm.equals(normalizeCategory(c.getName()))) continue;
            if (availableVariants(p).isEmpty()) continue;   // seulement le disponible
            String img = p.getImages().stream()
                    .min(Comparator.comparingInt(ProductImage::getPosition))
                    .map(ProductImage::getUrl).orElse(null);
            res.add(new FeedProductResponse(b.getSlug(), b.getName(), b.getLogoUrl(), p.getId(), p.getName(), p.getSalePrice(), img));
        }
        return res;
    }

    /** Normalisation d'un nom de categorie : minuscules + trim + espaces internes compresses. */
    private static String normalizeCategory(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    /**
     * Resout les infos PUBLIQUES (cartes) d'un ensemble de produits par leur id, cross-boutique
     * (ex. liste des favoris d'un utilisateur : ses produits peuvent venir de plusieurs boutiques).
     * Chaque boutique est lue sous sa propre RLS -> aucune donnee sensible d'autrui. Champs safe.
     */
    public List<FeedProductResponse> publicProductsByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        Set<Long> idSet = new HashSet<>(ids);
        List<Boutique> shops = boutiqueRepository.searchActive("");
        List<FeedProductResponse> all = new ArrayList<>();
        for (Boutique b : shops) {
            TenantContext.set(b.getId());
            try {
                all.addAll(self.productsByIdsForShop(b, idSet));
            } catch (RuntimeException ignore) {
                // une boutique en erreur ne casse pas la liste
            } finally {
                TenantContext.clear();
            }
        }
        all.sort(Comparator.comparing(FeedProductResponse::productId).reversed());
        return all;
    }

    /** Produits du tenant courant dont l'id est dans {@code ids}. Via proxy (RLS posee). */
    @Transactional(readOnly = true)
    public List<FeedProductResponse> productsByIdsForShop(Boutique b, Set<Long> ids) {
        List<FeedProductResponse> res = new ArrayList<>();
        for (Product p : productRepository.findAllById(ids)) {   // RLS -> sous-ensemble de CE tenant
            String img = p.getImages().stream()
                    .min(Comparator.comparingInt(ProductImage::getPosition))
                    .map(ProductImage::getUrl).orElse(null);
            res.add(new FeedProductResponse(b.getSlug(), b.getName(), b.getLogoUrl(), p.getId(), p.getName(), p.getSalePrice(), img));
        }
        return res;
    }

    /** Annuaire public : boutiques ACTIVES correspondant a la recherche (boutiques = hors RLS). */
    @Transactional(readOnly = true)
    public List<ShopResponse> search(String query) {
        return boutiqueRepository.searchActive(query != null ? query.trim() : "")
                .stream().map(ShopResponse::of).toList();
    }

    /**
     * Fiche publique d'UNE boutique ACTIVE par slug (nom + logo + following) — vitrine / lien partage.
     * {@code userId} nullable (visiteur anonyme) -> following = false.
     */
    @Transactional(readOnly = true)
    public ShopResponse getBySlug(String slug, Long userId) {
        Boutique b = boutiqueRepository.findBySlug(slug)
                .filter(x -> x.getStatus() == BoutiqueStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", slug));
        return ShopResponse.of(b, followService.isFollowing(userId, b.getId()));
    }

    /**
     * Statistiques publiques d'une boutique (profil « façon TikTok ») : abonnés (global), ventes et
     * produits (scopés au tenant via RLS -> comptés dans une transaction dédiée à cette boutique).
     */
    public ShopStatsResponse stats(String slug) {
        Boutique b = boutiqueRepository.findBySlug(slug)
                .filter(x -> x.getStatus() == BoutiqueStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", slug));
        long followers = followService.followersCount(b.getId());
        TenantContext.set(b.getId());
        try {
            return self.shopScopedStats(followers);   // ventes + produits sous RLS de cette boutique
        } finally {
            TenantContext.clear();
        }
    }

    /** Comptes SCOPÉS au tenant courant (ventes + produits). Appelé via le proxy (RLS posée). */
    @Transactional(readOnly = true)
    public ShopStatsResponse shopScopedStats(long followers) {
        return new ShopStatsResponse(followers, saleRepository.count(), productRepository.count());
    }

    /**
     * Resout la boutique ACTIVE du slug et POSE le tenant courant. A appeler AVANT une requete sur
     * une table tenant (l'aspect lira ce tenant a l'ouverture de la transaction suivante).
     */
    public Boutique enterShop(String slug) {
        Boutique b = boutiqueRepository.findBySlug(slug)
                .filter(x -> x.getStatus() == BoutiqueStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", slug));
        TenantContext.set(b.getId());
        return b;
    }

    /**
     * Données OG (safe) d'un produit du tenant courant (l'appelant a déjà posé le tenant via
     * {@link #enterShop}). RLS -> 404 si le produit n'appartient pas à cette boutique. Champs publics.
     */
    @Transactional(readOnly = true)
    public ProductOgData productOgScoped(Long productId) {
        Product p = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Produit", productId));
        String img = p.getImages().stream()
                .min(Comparator.comparingInt(ProductImage::getPosition))
                .map(ProductImage::getUrl).orElse(null);
        String cat = p.getCategory() != null ? p.getCategory().getName() : null;
        return new ProductOgData(p.getId(), p.getName(), p.getSalePrice(), cat, img);
    }

    /** Catalogue public du tenant courant (declinaisons disponibles + galerie, regroupees par produit). */
    @Transactional(readOnly = true)
    public List<PublicProductResponse> catalog() {
        Map<Long, ProductAcc> byProduct = new LinkedHashMap<>();
        for (ProductVariant v : variantRepository.findAll()) {   // RLS -> variantes du tenant courant
            if (v.getQuantity() == null || v.getQuantity() <= 0) continue;   // seulement le disponible
            ProductAcc acc = byProduct.computeIfAbsent(v.getProduct().getId(), id -> new ProductAcc(v));
            acc.variants.add(new PublicVariantResponse(
                    v.getId(), v.getColor().getName(), v.getColor().getHex(), v.getSize().getLabel(), v.getQuantity()));
        }
        return byProduct.values().stream()
                .map(a -> toResponse(a.first.getProduct(), a.variants))
                .toList();
    }

    /**
     * Galerie "photos" paginee du tenant courant : produits les plus recents d'abord, avec leur
     * galerie d'images. Pagination obligatoire (le grid peut etre long) ; RLS -> ce tenant seulement.
     */
    @Transactional(readOnly = true)
    public PageResponse<PublicProductResponse> gallery(int page, int size) {
        Page<Product> products = productRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));   // RLS -> tenant courant
        List<PublicProductResponse> content = products.getContent().stream()
                .map(p -> toResponse(p, availableVariants(p)))
                .toList();
        return PageResponse.of(products, content);
    }

    // --------------------------------- mappers ---------------------------------

    private List<PublicVariantResponse> availableVariants(Product p) {
        return variantRepository.findByProductId(p.getId()).stream()
                .filter(v -> v.getQuantity() != null && v.getQuantity() > 0)
                .map(v -> new PublicVariantResponse(v.getId(), v.getColor().getName(), v.getColor().getHex(), v.getSize().getLabel(), v.getQuantity()))
                .toList();
    }

    private PublicProductResponse toResponse(Product p, List<PublicVariantResponse> variants) {
        List<PublicImage> images = p.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getPosition))
                .map(img -> new PublicImage(img.getUrl(), img.getPosition()))
                .toList();
        return new PublicProductResponse(p.getId(), p.getReference(), p.getName(), p.getSalePrice(), variants, images);
    }

    private static final class ProductAcc {
        final ProductVariant first;
        final List<PublicVariantResponse> variants = new ArrayList<>();
        ProductAcc(ProductVariant first) { this.first = first; }
    }
}
