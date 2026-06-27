package com.smartboutique.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/** Nouvel ordre de la galerie : la liste des ids d'images dans l'ordre voulu (position 0 = couverture). */
public record ImageReorderRequest(
        @NotEmpty(message = "La liste des images est obligatoire")
        List<Long> imageIds
) {
}
