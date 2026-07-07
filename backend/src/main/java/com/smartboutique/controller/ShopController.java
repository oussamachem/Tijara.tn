package com.smartboutique.controller;

import com.smartboutique.dto.*;
import com.smartboutique.security.UserPrincipal;
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

    /** Annuaire public : boutiques ACTIVES correspondant a la recherche. */
    @GetMapping
    public List<ShopResponse> search(@RequestParam(required = false) String query) {
        return shopService.search(query);
    }

    /** Catalogue public d'une boutique (declinaisons disponibles). */
    @GetMapping("/{slug}/products")
    public List<PublicProductResponse> catalog(@PathVariable String slug) {
        shopService.enterShop(slug);   // resout la boutique ACTIVE + pose le tenant
        return shopService.catalog();
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
