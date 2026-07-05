package com.smartboutique.repository;

import com.smartboutique.entity.SalePayment;
import com.smartboutique.entity.TenderMethod;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalePaymentRepository extends JpaRepository<SalePayment, Long> {

    /** Usage unique GLOBAL du ticket cadeau : le code a-t-il deja ete encaisse ? */
    boolean existsByMethodAndTicketCode(TenderMethod method, String ticketCode);
}
