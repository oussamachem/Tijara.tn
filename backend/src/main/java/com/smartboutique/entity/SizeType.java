package com.smartboutique.entity;

import java.util.List;
import java.util.stream.IntStream;

/**
 * Type d'axe taille porte par une categorie. Chaque type definit un catalogue FIXE
 * de tailles autorisees (valide cote serveur a la creation des variantes).
 */
public enum SizeType {

    /** Tailles lettres : S, M, L, XL, XXL. */
    LETTER(List.of("S", "M", "L", "XL", "XXL")),

    /** Pointures europeennes : 38 a 46. */
    SHOE_EU(IntStream.rangeClosed(38, 46).mapToObj(String::valueOf).toList()),

    /** Tailles numeriques : 1 a 12. */
    NUMERIC(IntStream.rangeClosed(1, 12).mapToObj(String::valueOf).toList()),

    /** Pas d'axe taille (ex. accessoires) : la variante = couleur seule (size = UNI). */
    NONE(List.of("UNI"));

    /** Valeur de taille utilisee quand la categorie n'a pas d'axe taille. */
    public static final String NO_SIZE = "UNI";

    private final List<String> allowedSizes;

    SizeType(List<String> allowedSizes) {
        this.allowedSizes = allowedSizes;
    }

    public List<String> allowedSizes() {
        return allowedSizes;
    }

    public boolean isAllowed(String size) {
        return allowedSizes.contains(size);
    }
}
