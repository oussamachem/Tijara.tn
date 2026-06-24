package com.smartboutique.dto;

/** Representation d'une categorie. */
public record CategoryResponse(
        Long id,
        String name,
        String description
) {
}
