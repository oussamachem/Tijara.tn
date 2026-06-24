package com.smartboutique.repository;

import com.smartboutique.dto.ReturnResponse;
import com.smartboutique.entity.Return;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface ReturnRepository extends JpaRepository<Return, Long>, JpaSpecificationExecutor<Return> {

    /** Quantite deja retournee pour un produit sur une vente donnee. */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Return r " +
            "WHERE r.sale.id = :saleId AND r.product.id = :productId")
    int sumReturnedBySaleAndProduct(@Param("saleId") Long saleId, @Param("productId") Long productId);

    /**
     * Valeur monetaire des retours sur une periode, valorisee au prix unitaire capture
     * sur la vente d'origine (jointure Return x SaleItem sur la meme vente et le meme produit).
     */
    @Query("SELECT COALESCE(SUM(r.quantity * si.unitPrice), 0) " +
            "FROM Return r, SaleItem si " +
            "WHERE si.sale = r.sale AND si.product = r.product " +
            "AND r.returnDate >= :start AND r.returnDate < :end")
    BigDecimal sumReturnValueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Historique pagine des retours (projection), filtre optionnel par periode. */
    // cast(...) sur les parametres nullable : indispensable pour PostgreSQL (type des bind params).
    @Query(value = "SELECT new com.smartboutique.dto.ReturnResponse(" +
            "r.id, r.sale.id, r.product.id, r.product.reference, r.product.name, r.quantity, r.reason, r.returnDate) " +
            "FROM Return r " +
            "WHERE (cast(:start as timestamp) IS NULL OR r.returnDate >= :start) " +
            "AND (cast(:end as timestamp) IS NULL OR r.returnDate < :end) " +
            "ORDER BY r.returnDate DESC",
            countQuery = "SELECT COUNT(r) FROM Return r " +
                    "WHERE (cast(:start as timestamp) IS NULL OR r.returnDate >= :start) " +
                    "AND (cast(:end as timestamp) IS NULL OR r.returnDate < :end)")
    Page<ReturnResponse> searchReturns(@Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       Pageable pageable);
}
