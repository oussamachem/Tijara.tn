package com.smartboutique.dto;

import com.smartboutique.entity.Boutique;

/** Boutique dans l'annuaire public (marketplace) + logo. */
public record ShopResponse(Long id, String name, String slug, String logoUrl) {
    public static ShopResponse of(Boutique b) {
        return new ShopResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl());
    }
}
