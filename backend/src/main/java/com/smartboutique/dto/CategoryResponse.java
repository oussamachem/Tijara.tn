package com.smartboutique.dto;

import com.smartboutique.entity.SizeType;

import java.util.List;

/** Representation d'une categorie, avec son type de taille et les tailles autorisees. */
public record CategoryResponse(
        Long id,
        String name,
        String description,
        SizeType sizeType,
        List<String> allowedSizes
) {
}
