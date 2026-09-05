package com.smartboutique.controller;

import com.smartboutique.dto.FeedProductResponse;
import com.smartboutique.security.UserPrincipal;
import com.smartboutique.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Favoris (wishlist) de l'utilisateur AUTHENTIFIÉ. Sous /api/me/** -> identité (tout user connecté,
 * sans X-Shop-Id), scopé au user courant : on ne voit/modifie que SES favoris, jamais ceux d'autrui.
 */
@RestController
@RequestMapping("/api/me/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /** Ids des produits favoris (pour afficher l'état plein/vide des cœurs). */
    @GetMapping
    public List<Long> ids(@AuthenticationPrincipal UserPrincipal principal) {
        return favoriteService.listProductIds(principal.getId());
    }

    /** Cartes produit (champs publics) des favoris — page « Mes favoris ». */
    @GetMapping("/products")
    public List<FeedProductResponse> products(@AuthenticationPrincipal UserPrincipal principal) {
        return favoriteService.listProducts(principal.getId());
    }

    /** Ajoute un produit aux favoris du user courant. */
    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable Long productId, @AuthenticationPrincipal UserPrincipal principal) {
        favoriteService.add(principal.getId(), productId);
    }

    /** Retire un produit des favoris du user courant. */
    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long productId, @AuthenticationPrincipal UserPrincipal principal) {
        favoriteService.remove(principal.getId(), productId);
    }
}
