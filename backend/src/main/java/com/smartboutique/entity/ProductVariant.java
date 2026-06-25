package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Declinaison d'un produit (couleur x taille) = SKU. Porte le STOCK et le QR Code.
 * Unicite (product_id, color_id, size).
 */
@Entity
@Table(name = "product_variant",
        uniqueConstraints = @UniqueConstraint(name = "uk_variant_product_color_size",
                columnNames = {"product_id", "color_id", "size"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "color_id", nullable = false)
    private Color color;

    /** Taille (valeur du catalogue du SizeType de la categorie, ou UNI si NONE). */
    @Column(nullable = false, length = 20)
    private String size;

    /** Stock de cette declinaison (la quantite vit ICI, plus sur le produit). */
    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    /**
     * Seuil d'alerte de CETTE variante. Choix Phase 9 (deviation assumee du §1 qui l'avait
     * retire) : le seuil suit le stock, donc il vit au grain variante. Defaut 0 -> seule la
     * rupture (quantity=0) declenche l'alerte tant que l'admin ne l'ajuste pas.
     */
    @Column(name = "seuil_alerte", nullable = false)
    @Builder.Default
    private Integer seuilAlerte = 0;

    /** Reference unique de la variante : REF-SIZE-COLORSLUG. */
    @Column(nullable = false, unique = true)
    private String reference;

    /** Contenu encode dans le QR Code (= la reference de la variante). */
    @Column(name = "qr_code", unique = true)
    private String qrCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
