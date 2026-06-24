package com.smartboutique.repository;

import com.smartboutique.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /** Quantite totale vendue d'un produit sur une vente donnee. */
    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM SaleItem si " +
            "WHERE si.sale.id = :saleId AND si.product.id = :productId")
    int sumQuantityBySaleAndProduct(@Param("saleId") Long saleId, @Param("productId") Long productId);
}
