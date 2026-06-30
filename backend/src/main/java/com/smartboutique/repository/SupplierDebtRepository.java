package com.smartboutique.repository;

import com.smartboutique.dto.DebtRow;
import com.smartboutique.entity.SupplierDebt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SupplierDebtRepository extends JpaRepository<SupplierDebt, Long> {

    boolean existsBySupplierId(Long supplierId);

    /** Recherche paginee : filtre fournisseur / periode (creation) / statut DERIVE (paye agrege en SQL). */
    @Query(value = "SELECT new com.smartboutique.dto.DebtRow("
            + " d.id, d.supplier.id, d.supplier.name, d.totalAmount,"
            + " COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d), 0),"
            + " d.dueDate, d.createdAt)"
            + " FROM SupplierDebt d"
            + " WHERE (:supplierId IS NULL OR d.supplier.id = :supplierId)"
            + " AND (cast(:from as timestamp) IS NULL OR d.createdAt >= :from)"
            + " AND (cast(:to as timestamp) IS NULL OR d.createdAt < :to)"
            + " AND (:status IS NULL"
            + "   OR (:status = 'UNPAID' AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) = 0)"
            + "   OR (:status = 'PAID' AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) >= d.totalAmount)"
            + "   OR (:status = 'PARTIAL'"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) > 0"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) < d.totalAmount))"
            + " ORDER BY d.createdAt DESC",
            countQuery = "SELECT COUNT(d) FROM SupplierDebt d"
            + " WHERE (:supplierId IS NULL OR d.supplier.id = :supplierId)"
            + " AND (cast(:from as timestamp) IS NULL OR d.createdAt >= :from)"
            + " AND (cast(:to as timestamp) IS NULL OR d.createdAt < :to)"
            + " AND (:status IS NULL"
            + "   OR (:status = 'UNPAID' AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) = 0)"
            + "   OR (:status = 'PAID' AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) >= d.totalAmount)"
            + "   OR (:status = 'PARTIAL'"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) > 0"
            + "        AND COALESCE((SELECT SUM(p.amount) FROM DebtPayment p WHERE p.debt = d),0) < d.totalAmount))")
    Page<DebtRow> search(@Param("supplierId") Long supplierId,
                         @Param("status") String status,
                         @Param("from") LocalDateTime from,
                         @Param("to") LocalDateTime to,
                         Pageable pageable);

    // ------------------------------- Agregats dashboard dettes -------------------------------

    @Query("SELECT COUNT(d) FROM SupplierDebt d")
    long countAll();

    @Query("SELECT COALESCE(SUM(d.totalAmount), 0) FROM SupplierDebt d")
    BigDecimal sumTotal();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DebtPayment p")
    BigDecimal sumPaid();
}
