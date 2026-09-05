package com.smartboutique.dto;

import jakarta.validation.constraints.Size;

/**
 * Réglages WhatsApp OWNER : numéro de contact + message par défaut du lien wa.me.
 * {@code contactPhone} vide/blanc = efface le numéro. {@code whatsappDefaultMessage} vide = fallback client.
 * La normalisation/validation du numéro se fait côté service.
 */
public record ShopContactRequest(
        @Size(max = 30, message = "Numéro trop long") String contactPhone,
        @Size(max = 500, message = "Message trop long (500 caractères max)") String whatsappDefaultMessage
) {
}
