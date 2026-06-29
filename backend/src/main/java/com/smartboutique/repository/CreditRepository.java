package com.smartboutique.repository;

import com.smartboutique.dto.CreditRow;
import com.smartboutique.entity.Credit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CreditRepository extends JpaRepository<Credit, Long> {

    boolean existsByCustomerId(Long customerId);

    /**
     * Recherche paginee : filtre client / periode (creation) / statut DERIVE.
     * Le "paye" est agrege en SQL (sous-requete) -> pas de N+1 ; reste/statut derives au service.
     */
    @Query(value = "SELECT new com.smartboutique.dto.CreditRow("
            + " c.id, c.customer.id, c.customer.name, c.sale.totalAmount,"
            + " COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c), 0),"
            + " c.dueDate, c.cancelled, c.createdAt)"
            + " FROM Credit c"
            + " WHERE (:customerId IS NULL OR c.customer.id = :customerId)"
            + " AND (cast(:from as timestamp) IS NULL OR c.createdAt >= :from)"
            + " AND (cast(:to as timestamp) IS NULL OR c.createdAt < :to)"
            + " AND (:status IS NULL"
            + "   OR (:status = 'CANCELLED' AND c.cancelled = true)"
            + "   OR (:status = 'UNPAID' AND c.cancelled = false"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) = 0)"
            + "   OR (:status = 'PAID' AND c.cancelled = false"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) >= c.sale.totalAmount)"
            + "   OR (:status = 'PARTIAL' AND c.cancelled = false"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) > 0"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) < c.sale.totalAmount))"
            + " ORDER BY c.createdAt DESC",
            countQuery = "SELECT COUNT(c) FROM Credit c"
            + " WHERE (:customerId IS NULL OR c.customer.id = :customerId)"
            + " AND (cast(:from as timestamp) IS NULL OR c.createdAt >= :from)"
            + " AND (cast(:to as timestamp) IS NULL OR c.createdAt < :to)"
            + " AND (:status IS NULL"
            + "   OR (:status = 'CANCELLED' AND c.cancelled = true)"
            + "   OR (:status = 'UNPAID' AND c.cancelled = false"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) = 0)"
            + "   OR (:status = 'PAID' AND c.cancelled = false"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) >= c.sale.totalAmount)"
            + "   OR (:status = 'PARTIAL' AND c.cancelled = false"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) > 0"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM CreditPayment p WHERE p.credit = c),0) < c.sale.totalAmount))")
    Page<CreditRow> search(@Param("customerId") Long customerId,
                           @Param("status") String status,
                           @Param("from") LocalDateTime from,
                           @Param("to") LocalDateTime to,
                           Pageable pageable);

    // ------------------------------- Agregats dashboard (credits actifs) -------------------------------

    @Query("SELECT COUNT(c) FROM Credit c WHERE c.cancelled = false")
    long countActive();

    @Query("SELECT COALESCE(SUM(c.sale.totalAmount), 0) FROM Credit c WHERE c.cancelled = false")
    BigDecimal sumTotalActive();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CreditPayment p WHERE p.credit.cancelled = false")
    BigDecimal sumCollectedActive();
}
