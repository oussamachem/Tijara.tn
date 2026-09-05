package com.smartboutique.controller;

import com.smartboutique.dto.FeedProductResponse;
import com.smartboutique.dto.PageResponse;
import com.smartboutique.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Marketplace GLOBALE (accueil client) — lecture PUBLIQUE, cross-boutique.
 * <ul>
 *   <li>Barre de categories : noms de categories distincts presents dans la marketplace.</li>
 *   <li>Fil d'une categorie : produits de TOUTES les boutiques actives, champs SAFE uniquement
 *       (nom produit, prix de vente, image de couverture, boutique) — jamais de cout/marge/
 *       fournisseur/stock interne d'autrui.</li>
 * </ul>
 * Le regroupement se fait par NOM de categorie (insensible casse/espaces) — MVP, pas de referentiel.
 */
@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final ShopService shopService;

    /** Noms de categories distincts (avec produits disponibles) presents dans la marketplace. */
    @GetMapping("/categories")
    public List<String> categories() {
        return shopService.marketplaceCategories();
    }

    /** Fil pagine d'une categorie (par nom), cross-boutique. Scroll infini cote client. */
    @GetMapping("/products")
    public PageResponse<FeedProductResponse> products(@RequestParam String category,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "24") int size) {
        return shopService.marketplaceProducts(category, Math.max(page, 0), Math.min(Math.max(size, 1), 48));
    }
}
