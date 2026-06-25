package com.smartboutique.repository;

import com.smartboutique.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository
        extends JpaRepository<ProductVariant, Long>, JpaSpecificationExecutor<ProductVariant> {

    Optional<ProductVariant> findByReference(String reference);

    Optional<ProductVariant> findByQrCode(String qrCode);

    boolean existsByReference(String reference);

    /** Garde-fou suppression couleur : refuser si une variante l'utilise. */
    boolean existsByColorId(Long colorId);

    /** Nombre de variantes d'un produit (invariant : on ne supprime pas la derniere). */
    long countByProductId(Long productId);

    List<ProductVariant> findByProductId(Long productId);

    /** Une declinaison (couleur+taille) existe-t-elle deja pour ce produit ? */
    boolean existsByProductIdAndColorIdAndSize(Long productId, Long colorId, String size);

    /**
     * Decrement atomique conditionnel du stock de la VARIANTE (anti-survente concurrente).
     * 0 ligne modifiee => stock insuffisant.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.quantity = v.quantity - :qty WHERE v.id = :id AND v.quantity >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    /** Reincremente le stock de la variante (retour). */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.quantity = v.quantity + :qty WHERE v.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    // ----------------------------- Agregats tableau de bord -----------------------------

    /** Stock total = somme des quantites de toutes les variantes. */
    @Query("SELECT COALESCE(SUM(v.quantity), 0) FROM ProductVariant v")
    long sumAllQuantities();

    /** Variantes en rupture (0) ou sous leur seuil d'alerte. */
    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.quantity <= v.seuilAlerte")
    long countLowStockVariants();

    /** Produits ayant au moins une variante en rupture/sous seuil. */
    @Query("SELECT COUNT(DISTINCT v.product.id) FROM ProductVariant v WHERE v.quantity <= v.seuilAlerte")
    long countProductsWithLowStock();

    /** Variantes en rupture/sous seuil (pour la liste des alertes). */
    @Query("SELECT v FROM ProductVariant v WHERE v.quantity <= v.seuilAlerte ORDER BY v.quantity ASC")
    List<ProductVariant> findLowStockVariants();
}
