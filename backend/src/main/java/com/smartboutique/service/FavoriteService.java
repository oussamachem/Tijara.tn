package com.smartboutique.service;

import com.smartboutique.dto.FeedProductResponse;
import com.smartboutique.entity.Favorite;
import com.smartboutique.repository.FavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Favoris (wishlist) d'un utilisateur. Table GLOBALE (identité) : chaque opération est scopée au
 * user courant (l'utilisateur ne voit/modifie que SES favoris). Les détails produits (page « Mes
 * favoris ») sont résolus cross-boutique en champs PUBLICS via {@link ShopService}.
 */
@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ShopService shopService;

    /** Ajoute un produit aux favoris du user (idempotent). */
    @Transactional
    public void add(Long userId, Long productId) {
        if (!favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            favoriteRepository.save(Favorite.builder().userId(userId).productId(productId).build());
        }
    }

    /** Retire un produit des favoris du user (idempotent). */
    @Transactional
    public void remove(Long userId, Long productId) {
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
    }

    /** Ids des produits favoris du user (pour l'état des cœurs côté front). */
    @Transactional(readOnly = true)
    public List<Long> listProductIds(Long userId) {
        return favoriteRepository.findProductIdsByUserId(userId);
    }

    /** Cartes produit (champs publics) des favoris du user — page « Mes favoris ». */
    public List<FeedProductResponse> listProducts(Long userId) {
        return shopService.publicProductsByIds(favoriteRepository.findProductIdsByUserId(userId));
    }
}
