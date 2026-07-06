package com.smartboutique.repository;

import com.smartboutique.entity.ReservationPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ReservationPaymentRepository extends JpaRepository<ReservationPayment, Long> {

    /** Somme deja versee sur une reservation (source de verite du "reste"). */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM ReservationPayment p WHERE p.reservation.id = :reservationId")
    BigDecimal sumPaidByReservation(@Param("reservationId") Long reservationId);

    /** Versements d'une reservation, dans l'ordre chronologique (tenders de la vente de cloture). */
    List<ReservationPayment> findByReservationIdOrderByCreatedAtAsc(Long reservationId);
}
