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

    /** Quantite deja retournee pour une VARIANTE sur une vente donnee. */
    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM Return r " +
            "WHERE r.sale.id = :saleId AND r.variant.id = :variantId")
    int sumReturnedBySaleAndVariant(@Param("saleId") Long saleId, @Param("variantId") Long variantId);

    /**
     * Valeur monetaire des retours sur une periode, valorisee au prix unitaire capture sur la
     * vente d'origine (jointure Return x SaleItem sur la meme vente et la meme variante).
     */
    @Query("SELECT COALESCE(SUM(r.quantity * si.unitPrice), 0) " +
            "FROM Return r, SaleItem si " +
            "WHERE si.sale = r.sale AND si.variant = r.variant " +
            "AND r.returnDate >= :start AND r.returnDate < :end")
    BigDecimal sumReturnValueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** Historique pagine des retours (projection), filtre optionnel par periode. */
    @Query(value = "SELECT new com.smartboutique.dto.ReturnResponse(" +
            "r.id, r.sale.id, r.variant.id, r.variant.reference, r.variant.product.name, " +
            "r.variant.color.name, r.variant.size.label, r.quantity, r.reason, r.returnDate) " +
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
