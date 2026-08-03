package com.smartboutique.controller;

import com.smartboutique.dto.*;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.BoutiqueService;
import com.smartboutique.service.OrderService;
import com.smartboutique.service.ShopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Marketplace CLIENT. Annuaire + catalogue = PUBLIC (pas de JWT). Commande = CLIENT authentifie.
 * Le tenant est resolu par le SLUG (ShopService.enterShop) : le client ne peut commander que dans
 * la boutique du slug, aux prix de cette boutique (recalcul serveur) -> pas de commande cross-boutique.
 */
@RestController
@RequestMapping("/api/shops")
@RequiredArgsConstructor
public class ShopController {

    private final ShopService shopService;
    private final OrderService orderService;
    private final BoutiqueService boutiqueService;

    /** Annuaire public : boutiques ACTIVES correspondant a la recherche. */
    @GetMapping
    public List<ShopResponse> search(@RequestParam(required = false) String query) {
        return shopService.search(query);
    }

    /** Fil marketplace public (accueil) : produits melanges de toutes les boutiques actives. */
    @GetMapping("/feed")
    public List<FeedProductResponse> feed(@RequestParam(defaultValue = "40") int limit) {
        return shopService.feed(Math.min(Math.max(limit, 1), 100));
    }

    /**
     * Self-service (Phase B) : l'utilisateur AUTHENTIFIE cree SA boutique et en devient OWNER.
     * Identite requise (pas de X-Shop-Id : on cree justement le tenant).
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoutiqueResponse createMyShop(@Valid @RequestBody CreateMyShopRequest request,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        return boutiqueService.createForOwner(request.name(), principal.getId());
    }

    /** Catalogue public d'une boutique (declinaisons disponibles + galerie photos). */
    @GetMapping("/{slug}/products")
    public List<PublicProductResponse> catalog(@PathVariable String slug) {
        shopService.enterShop(slug);   // resout la boutique ACTIVE + pose le tenant
        return shopService.catalog();
    }

    /** Galerie "photos" paginee d'une boutique (grid style Pinterest, defilement infini cote client). */
    @GetMapping("/{slug}/gallery")
    public PageResponse<PublicProductResponse> gallery(@PathVariable String slug,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "24") int size) {
        shopService.enterShop(slug);
        return shopService.gallery(page, Math.min(size, 60));   // borne dure : jamais 500 images d'un coup
    }

    /** Passer commande dans la boutique du slug (CLIENT). Total recalcule serveur. */
    @PostMapping("/{slug}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse order(@PathVariable String slug,
                               @Valid @RequestBody OrderCreateRequest request,
                               @AuthenticationPrincipal UserPrincipal principal) {
        shopService.enterShop(slug);
        return orderService.create(principal.getId(), request);
    }

    /** Suivi : les commandes du client dans cette boutique. */
    @GetMapping("/{slug}/orders/mine")
    public List<OrderResponse> myOrders(@PathVariable String slug,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        shopService.enterShop(slug);
        return orderService.listMine(principal.getId());
    }
}
