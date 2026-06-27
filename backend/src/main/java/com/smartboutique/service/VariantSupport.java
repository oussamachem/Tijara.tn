package com.smartboutique.service;

import org.springframework.stereotype.Component;

import java.text.Normalizer;

/**
 * Helpers partages entre la creation de produit et l'ajout de variante :
 * slug couleur et reference variante stable. Les tailles proviennent du catalogue
 * Tailles gere (table sizes) : la validation = la taille existe dans le catalogue
 * (resolue par id cote service), il n'y a plus de SizeType par categorie.
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

    /** Reference variante = REF-SIZESLUG-COLORSLUG (stable, unique). */
    public String buildVariantReference(String productReference, String sizeLabel, String colorName) {
        return productReference + "-" + slug(sizeLabel) + "-" + slug(colorName);
    }
}
