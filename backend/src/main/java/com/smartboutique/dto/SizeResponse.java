package com.smartboutique.dto;

/** Representation d'une taille du catalogue. */
public record SizeResponse(
        Long id,
        String label,
        Integer position
) {
}
