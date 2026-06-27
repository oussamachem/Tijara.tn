package com.smartboutique.dto;

/** Une photo de la galerie produit. */
public record ProductImageResponse(
        Long id,
        String url,
        int position
) {
}
