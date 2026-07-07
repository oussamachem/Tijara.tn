package com.smartboutique.dto;

import com.smartboutique.entity.Boutique;

/** Boutique dans l'annuaire public (marketplace). */
public record ShopResponse(Long id, String name, String slug) {
    public static ShopResponse of(Boutique b) {
        return new ShopResponse(b.getId(), b.getName(), b.getSlug());
    }
}
