package com.smartboutique.service;

import com.smartboutique.dto.ShopResponse;
import com.smartboutique.entity.Boutique;
import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.Follow;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.repository.BoutiqueRepository;
import com.smartboutique.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Abonnements client -> boutique (« following »). Identité (hors RLS). Sert au fil « Mes boutiques »
 * de l'accueil et au bouton Suivre de la vitrine.
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final BoutiqueRepository boutiqueRepository;

    private Boutique activeShop(String slug) {
        return boutiqueRepository.findBySlug(slug)
                .filter(b -> b.getStatus() == BoutiqueStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Boutique", slug));
    }

    /** Suivre une boutique (idempotent). */
    @Transactional
    public void follow(Long userId, String slug) {
        Boutique b = activeShop(slug);
        if (!followRepository.existsByUserIdAndShopId(userId, b.getId())) {
            followRepository.save(Follow.builder().userId(userId).shopId(b.getId()).build());
        }
    }

    /** Ne plus suivre (idempotent). */
    @Transactional
    public void unfollow(Long userId, String slug) {
        Boutique b = activeShop(slug);
        followRepository.deleteByUserIdAndShopId(userId, b.getId());
    }

    public boolean isFollowing(Long userId, Long shopId) {
        return userId != null && followRepository.existsByUserIdAndShopId(userId, shopId);
    }

    /** Nombre d'abonnés d'une boutique (profil public). */
    public long followersCount(Long shopId) {
        return followRepository.countByShopId(shopId);
    }

    /** Boutiques suivies par l'utilisateur (ACTIVES), triées par nom. */
    @Transactional(readOnly = true)
    public List<ShopResponse> myFollows(Long userId) {
        List<Long> shopIds = followRepository.findByUserId(userId).stream().map(Follow::getShopId).toList();
        if (shopIds.isEmpty()) return List.of();
        return boutiqueRepository.findAllById(shopIds).stream()
                .filter(b -> b.getStatus() == BoutiqueStatus.ACTIVE)
                .sorted(Comparator.comparing(Boutique::getName))
                .map(ShopResponse::of)
                .toList();
    }
}
