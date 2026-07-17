package com.smartboutique.dto;

import com.smartboutique.entity.BoutiqueStatus;
import com.smartboutique.entity.ShopMemberRole;

/**
 * Une appartenance (membership) de l'utilisateur connecte : la boutique + son role contextuel.
 * Sert au front unifie a aiguiller vers l'espace OWNER ou VENDOR et a alimenter le selecteur
 * de boutique (X-Shop-Id = {@code shopId}). Lecture seule, au grain identite (pas de X-Shop-Id).
 */
public record MembershipResponse(
        Long shopId,
        String name,
        String slug,
        ShopMemberRole role,
        BoutiqueStatus status
) {
}
