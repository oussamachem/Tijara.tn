package com.smartboutique.repository;

import com.smartboutique.dto.SaleSummaryResponse;
import com.smartboutique.dto.TopProduct;
import com.smartboutique.entity.Sale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SaleRepository extends JpaRepository<Sale, Long>, JpaSpecificationExecutor<Sale> {

    /**
     * Detail d'une vente sans N+1 : charge vendeur et lignes en une requete. Les attributs
     * variante (reference, produit, couleur, taille) sont denormalises sur les lignes -> pas
     * besoin de charger les variantes pour l'affichage.
     */
    @EntityGraph(attributePaths = {"seller", "items"})
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findDetailById(@Param("id") Long id);

    // ----------------------------- Agregats tableau de bord -----------------------------

    @Query("SELECT COUNT(s) FROM Sale s WHERE s.saleDate >= :start AND s.saleDate < :end")
    long countBySaleDateBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(SUM(s.totalAmount), 0) FROM Sale s WHERE s.saleDate >= :start AND s.saleDate < :end")
    BigDecimal sumTotalAmountBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Meilleures ventes par quantite, agregees au PRODUIT (roll-up des variantes). */
    @Query("SELECT new com.smartboutique.dto.TopProduct(" +
            "si.variant.product.id, si.variant.product.reference, si.variant.product.name, " +
            "SUM(si.quantity), SUM(si.totalPrice)) " +
            "FROM SaleItem si " +
            "GROUP BY si.variant.product.id, si.variant.product.reference, si.variant.product.name " +
            "ORDER BY SUM(si.quantity) DESC")
    List<TopProduct> findTopSellingProducts(Pageable pageable);

    // --------------------------------- Historique des ventes ----------------------------

    /**
     * Historique pagine (projection legere, sans charger les lignes -> pas de N+1).
     * Filtres optionnels : periode [start, end[ et vendeur.
     */
    // Note : les parametres nullable sont types via cast(...) dans les tests "IS NULL",
    // sinon PostgreSQL ne peut pas inferer leur type ("could not determine data type of parameter").
    @Query(value = "SELECT new com.smartboutique.dto.SaleSummaryResponse(" +
            "s.id, s.seller.id, s.seller.fullName, s.saleDate, s.paymentMethod, s.discount, s.totalAmount, " +
            "(SELECT COUNT(si) FROM SaleItem si WHERE si.sale = s)) " +
            "FROM Sale s " +
            "WHERE (cast(:start as timestamp) IS NULL OR s.saleDate >= :start) " +
            "AND (cast(:end as timestamp) IS NULL OR s.saleDate < :end) " +
            "AND (cast(:sellerId as long) IS NULL OR s.seller.id = :sellerId) " +
            "ORDER BY s.saleDate DESC",
            countQuery = "SELECT COUNT(s) FROM Sale s " +
                    "WHERE (cast(:start as timestamp) IS NULL OR s.saleDate >= :start) " +
                    "AND (cast(:end as timestamp) IS NULL OR s.saleDate < :end) " +
                    "AND (cast(:sellerId as long) IS NULL OR s.seller.id = :sellerId)")
    Page<SaleSummaryResponse> searchHistory(@Param("start") LocalDateTime start,
                                            @Param("end") LocalDateTime end,
                                            @Param("sellerId") Long sellerId,
                                            Pageable pageable);
}
