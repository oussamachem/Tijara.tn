package com.smartboutique.repository;

import com.smartboutique.entity.DebtPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface DebtPaymentRepository extends JpaRepository<DebtPayment, Long> {

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM DebtPayment p WHERE p.debt.id = :debtId")
    BigDecimal sumPaidByDebt(@Param("debtId") Long debtId);

    /** Garde-fou suppression : refuser si la dette a deja des paiements (historique). */
    boolean existsByDebtId(Long debtId);
}
