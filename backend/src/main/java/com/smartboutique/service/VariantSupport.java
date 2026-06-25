package com.smartboutique.service;

import com.smartboutique.entity.Category;
import com.smartboutique.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Helpers partages entre la creation de produit et l'ajout de variante :
 * slug couleur, reference variante stable, validation de la taille selon la categorie.
 */
@Component
public class VariantSupport {

    /** MAJUSCULE, sans accents ni espaces (ex. "Bleu marine" -> "BLEU-MARINE"). */
    public String slug(String value) {
        if (value == null) return "";
        String noAccents = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return noAccents.toUpperCase()
                .replaceAll("[^A-Z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    /** Reference variante = REF-SIZE-COLORSLUG (stable, unique). */
    public String buildVariantReference(String productReference, String size, String colorName) {
        return productReference + "-" + size.toUpperCase() + "-" + slug(colorName);
    }

    /** Refuse une taille non conforme au SizeType de la categorie. */
    public void validateSize(Category category, String size) {
        if (!category.getSizeType().isAllowed(size)) {
            throw new BusinessException(
                    "Taille '" + size + "' invalide pour la categorie '" + category.getName()
                            + "' (type " + category.getSizeType() + ", autorisees : "
                            + category.getSizeType().allowedSizes() + ")",
                    HttpStatus.BAD_REQUEST);
        }
    }
}
