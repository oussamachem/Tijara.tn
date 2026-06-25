package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne d'une vente : une VARIANTE (declinaison couleur x taille), une quantite, son prix.
 * Les attributs d'affichage (reference variante, nom produit, couleur, taille) sont
 * DENORMALISES pour figer l'historique meme si la variante evolue ensuite.
 */
@Entity
@Table(name = "sale_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    /** Prix unitaire (prix produit) capture au moment de la vente. */
    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "total_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalPrice;

    // --- Denormalisation (figee a la vente, pour l'historique) ---
    @Column(name = "variant_reference", nullable = false)
    private String variantReference;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "color_name")
    private String colorName;

    @Column(name = "size", length = 20)
    private String size;
}
