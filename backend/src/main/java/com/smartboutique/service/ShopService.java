package com.smartboutique.service;

import com.smartboutique.dto.FeedProductResponse;
import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.PublicProductResponse;
import com.smartboutique.dto.PublicProductResponse.PublicImage;
import com.smartboutique.dto.PublicProductResponse.PublicVariantResponse;
import com.smartboutique.dto.ShopResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.ProductImage;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ProductRepository;
import com.smartboutique.repository.ProductVariantRepository;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Annuaire public : boutiques ACTIVES correspondant a la recherche (boutiques = hors RLS). */
    @Transactional(readOnly = true)
    public List<ShopResponse> search(String query) {
        return boutiqueRepository.searchActive(query != null ? query.trim() : "")
                .stream().map(ShopResponse::of).toList();
    }

    /** Fiche publique d'UNE boutique ACTIVE par slug (nom + logo) — pour la vitrine / lien partage. */
    @Transactional(readOnly = true)
    public ShopResponse getBySlug(String slug) {
        return ShopResponse.of(boutiqueRepository.findBySlug(slug)
                .filter(b -> b.getStatus() == BoutiqueStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", slug)));
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

    /** Catalogue public du tenant courant (declinaisons disponibles + galerie, regroupees par produit). */
    @Transactional(readOnly = true)
    public List<PublicProductResponse> catalog() {
        Map<Long, ProductAcc> byProduct = new LinkedHashMap<>();
        for (ProductVariant v : variantRepository.findAll()) {   // RLS -> variantes du tenant courant
            if (v.getQuantity() == null || v.getQuantity() <= 0) continue;   // seulement le disponible
            ProductAcc acc = byProduct.computeIfAbsent(v.getProduct().getId(), id -> new ProductAcc(v));
            acc.variants.add(new PublicVariantResponse(
                    v.getId(), v.getColor().getName(), v.getSize().getLabel(), v.getQuantity()));
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
                .map(v -> new PublicVariantResponse(v.getId(), v.getColor().getName(), v.getSize().getLabel(), v.getQuantity()))
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
