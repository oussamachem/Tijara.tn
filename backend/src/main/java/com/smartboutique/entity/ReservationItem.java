package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Ligne d'une reservation : une VARIANTE, une quantite, son prix capture a la creation.
 * Les attributs d'affichage sont DENORMALISES (comme {@link SaleItem}) pour figer l'historique
 * et alimenter directement la vente de cloture.
 */
@Entity
@Table(name = "reservation_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", precision = 12, scale = 2, nullable = false)
    private BigDecimal unitPrice;

    // --- Denormalisation (figee a la creation) ---
    @Column(name = "variant_reference", nullable = false)
    private String variantReference;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "color_name")
    private String colorName;

    @Column(name = "size", length = 20)
    private String size;
}
