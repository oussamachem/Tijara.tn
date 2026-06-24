package com.smartboutique.dto;

import java.math.BigDecimal;

/** Produit figurant dans le palmares des meilleures ventes (par quantite vendue). */
public record TopProduct(
        Long productId,
        String reference,
        String name,
        Long quantitySold,
        BigDecimal revenue
) {
}
