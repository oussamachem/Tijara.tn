package com.smartboutique.dto;

import java.math.BigDecimal;

/**
 * Données PUBLIQUES (safe-fields) d'un produit pour l'aperçu Open Graph : nom, prix de vente,
 * catégorie, image de couverture. JAMAIS de coût/marge/stock interne.
 */
public record ProductOgData(Long productId, String name, BigDecimal price, String category, String imageUrl) {
}
