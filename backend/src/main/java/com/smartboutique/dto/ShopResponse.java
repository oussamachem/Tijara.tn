package com.smartboutique.dto;

import com.smartboutique.entity.Boutique;

/**
 * Boutique dans le marketplace (annuaire / vitrine) + logo. Champs PUBLICS non sensibles :
 * {@code contactPhone} = numéro WhatsApp (format international, null si non renseigné) ;
 * {@code whatsappDefaultMessage} = message par défaut du lien wa.me (null -> fallback client).
 * {@code following} = l'utilisateur connecté suit-il cette boutique (null si non pertinent).
 */
public record ShopResponse(Long id, String name, String slug, String logoUrl,
                           String contactPhone, String whatsappDefaultMessage, Boolean following) {
    public static ShopResponse of(Boutique b) {
        return new ShopResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(),
                b.getContactPhone(), b.getWhatsappDefaultMessage(), null);
    }

    public static ShopResponse of(Boutique b, boolean following) {
        return new ShopResponse(b.getId(), b.getName(), b.getSlug(), b.getLogoUrl(),
                b.getContactPhone(), b.getWhatsappDefaultMessage(), following);
    }
}
