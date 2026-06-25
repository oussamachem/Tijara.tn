package com.smartboutique.repository;

import com.smartboutique.entity.SaleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    /** Quantite totale vendue d'une VARIANTE sur une vente donnee. */
    @Query("SELECT COALESCE(SUM(si.quantity), 0) FROM SaleItem si " +
            "WHERE si.sale.id = :saleId AND si.variant.id = :variantId")
    int sumQuantityBySaleAndVariant(@Param("saleId") Long saleId, @Param("variantId") Long variantId);

    /** Une variante a-t-elle deja ete vendue ? (garde-fou suppression variante). */
    boolean existsByVariant_Id(Long variantId);

    /** Un produit a-t-il deja ete vendu (via une de ses variantes) ? (garde-fou suppression produit). */
    boolean existsByVariant_Product_Id(Long productId);
}
