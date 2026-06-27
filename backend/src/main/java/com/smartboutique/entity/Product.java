package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Modele d'article (ex. « chemise »). Le stock et le QR Code vivent au niveau des
 * {@link ProductVariant} (couleur x taille). Le prix reste au niveau produit (cf. decision Phase 9).
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference de base du modele (les variantes en derivent : REF-SIZE-COLOR). */
    @Column(nullable = false, unique = true)
    private String reference;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "purchase_price", precision = 12, scale = 2)
    private BigDecimal purchasePrice;

    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Declinaisons du produit (couleur x taille). Un produit a TOUJOURS au moins une variante. */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariant> variants = new ArrayList<>();

    /**
     * Galerie de photos du produit, triee par position. La position 0 est la couverture
     * (exposee en imageUrl dans le DTO pour la non-regression mono-image : liste, mobile).
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    public void addVariant(ProductVariant variant) {
        variants.add(variant);
        variant.setProduct(this);
    }

    // ----------------------------- Helpers galerie ------------------------------

    /** Ajoute une image en fin de galerie (position = taille courante) et lie les deux cotes. */
    public ProductImage addImage(String url) {
        ProductImage image = ProductImage.builder()
                .product(this)
                .url(url)
                .position(images.size())
                .build();
        images.add(image);
        return image;
    }

    /** URL de la couverture (image de position la plus basse), ou null si aucune photo. */
    public String getCoverUrl() {
        return images.stream()
                .min(java.util.Comparator.comparingInt(ProductImage::getPosition))
                .map(ProductImage::getUrl)
                .orElse(null);
    }

    /** Renumerote les positions 0..n-1 selon l'ordre courant (apres suppression/reordre). */
    public void normalizePositions() {
        images.sort(java.util.Comparator.comparingInt(ProductImage::getPosition));
        for (int i = 0; i < images.size(); i++) {
            images.get(i).setPosition(i);
        }
    }
}
