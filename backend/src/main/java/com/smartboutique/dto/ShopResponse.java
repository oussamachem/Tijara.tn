package com.smartboutique.dto;

import com.smartboutique.entity.Boutique;

/**
 * Boutique dans le marketplace (annuaire / vitrine) + logo. {@code following} = l'utilisateur
 * connecté suit-il cette boutique (null si non pertinent : annuaire/fil, ou visiteur anonyme).
 */
public record ShopResponse(Long id, String name, String slug, String logoUrl, Boolean following) {
    public static ShopResponse of(Boutique b) {
        return new ShopResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(), null);
    }

    public static ShopResponse of(Boutique b, boolean following) {
        return new ShopResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(), following);
    }
}
