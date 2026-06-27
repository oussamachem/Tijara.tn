package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Une photo d'un produit (galerie). La photo de <b>position 0</b> est la couverture.
 * Stockee comme un fichier sur disque (cf. {@code FileStorageService}) ; {@code url}
 * contient le chemin relatif servi sous {@code /uploads/**}.
 */
@Entity
@Table(name = "product_images", indexes = {
        @Index(name = "idx_product_images_product_position", columnList = "product_id, position")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String url;

    /** Ordre dans la galerie ; 0 = couverture. */
    @Column(name = "position", nullable = false)
    private Integer position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
