package com.smartboutique.service;

import com.smartboutique.dto.PublicProductResponse;
import com.smartboutique.dto.PublicProductResponse.PublicVariantResponse;
import com.smartboutique.dto.ShopResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.ProductVariant;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.ProductVariantRepository;
import com.smartboutique.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Marketplace public. La resolution du tenant se fait par le SLUG (pas par JWT) : {@link #enterShop}
 * pose le {@link TenantContext} a partir de la boutique du slug AVANT toute requete sur des tables
 * tenant (produits, commandes) -> l'aspect pose le GUC -> la RLS scope au bon tenant.
 */
@Service
@RequiredArgsConstructor
public class ShopService {

    private final BoutiqueRepository boutiqueRepository;
    private final ProductVariantRepository variantRepository;

    /** Annuaire public : boutiques ACTIVES correspondant a la recherche (boutiques = hors RLS). */
    @Transactional(readOnly = true)
    public List<ShopResponse> search(String query) {
        return boutiqueRepository.searchActive(query != null ? query.trim() : "")
                .stream().map(ShopResponse::of).toList();
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

    /** Catalogue public du tenant courant (declinaisons disponibles, regroupees par produit). */
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
                .map(a -> new PublicProductResponse(
                        a.first.getProduct().getId(), a.first.getProduct().getReference(),
                        a.first.getProduct().getName(), a.first.getProduct().getSalePrice(), a.variants))
                .toList();
    }

    private static final class ProductAcc {
        final ProductVariant first;
        final List<PublicVariantResponse> variants = new ArrayList<>();
        ProductAcc(ProductVariant first) { this.first = first; }
    }
}
