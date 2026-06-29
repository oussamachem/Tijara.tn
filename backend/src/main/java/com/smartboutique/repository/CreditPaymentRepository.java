package com.smartboutique.repository;

import com.smartboutique.entity.CreditPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface CreditPaymentRepository extends JpaRepository<CreditPayment, Long> {

    /** Somme deja payee sur un credit (source de verite du "reste"). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM CreditPayment p WHERE p.credit.id = :creditId")
    BigDecimal sumPaidByCredit(@Param("creditId") Long creditId);
}
