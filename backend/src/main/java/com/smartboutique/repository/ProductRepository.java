package com.smartboutique.repository;

import com.smartboutique.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByReference(String reference);

    Optional<Product> findByQrCode(String qrCode);

    boolean existsByReference(String reference);

    /** Vrai si au moins un produit est rattache a la categorie donnee (garde-fou suppression). */
    boolean existsByCategoryId(Long categoryId);

    /**
     * Decremente le stock de facon atomique uniquement s'il est suffisant.
     * Renvoie le nombre de lignes modifiees : 0 => stock insuffisant (anti-survente concurrente).
     */
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity - :qty WHERE p.id = :id AND p.quantity >= :qty")
    int decrementStockIfAvailable(@Param("id") Long id, @Param("qty") int qty);

    /** Reincremente le stock (retour de produit). */
    @Modifying
    @Query("UPDATE Product p SET p.quantity = p.quantity + :qty WHERE p.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") int qty);

    // ----------------------------- Agregats tableau de bord -----------------------------

    /** Stock total (somme des quantites) — agrege en SQL. */
    @Query("SELECT COALESCE(SUM(p.quantity), 0) FROM Product p")
    long sumAllQuantities();

    /** Nombre de produits en rupture ou sous le seuil d'alerte. */
    @Query("SELECT COUNT(p) FROM Product p WHERE p.quantity <= p.seuilAlerte")
    long countLowStock();
}
