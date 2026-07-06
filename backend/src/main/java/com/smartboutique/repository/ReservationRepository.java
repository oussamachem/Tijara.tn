package com.smartboutique.repository;

import com.smartboutique.dto.ReservationRow;
import com.smartboutique.entity.Reservation;
import com.smartboutique.entity.ReservationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    boolean existsByReference(String reference);

    /**
     * Detail d'une reservation sans N+1 sur les lignes (vendeur + items charges en une requete).
     * Les versements sont charges paresseusement dans la meme transaction (evite le
     * MultipleBagFetchException d'un double fetch de collections).
     */
    @EntityGraph(attributePaths = {"seller", "items"})
    @Query("SELECT r FROM Reservation r WHERE r.id = :id")
    Optional<Reservation> findDetailById(@Param("id") Long id);

    /** Reservations a expirer : ACTIVE dont l'echeance est depassee (job quotidien). */
    List<Reservation> findByStatusAndDueDateBefore(ReservationStatus status, LocalDateTime now);

    // ------------------------- Listes (projection : paye deja agrege) -------------------------

    String ROW = "new com.smartboutique.dto.ReservationRow(" +
            "r.id, r.reference, r.customerName, r.customerPhone, r.status, r.totalAmount, " +
            "COALESCE((SELECT SUM(p.amount) FROM ReservationPayment p WHERE p.reservation = r), 0), " +
            "r.dueDate, r.depositForfeited, r.createdAt)";

    @Query("SELECT " + ROW + " FROM Reservation r ORDER BY r.dueDate ASC")
    List<ReservationRow> findAllRows();

    @Query("SELECT " + ROW + " FROM Reservation r WHERE r.status = :status ORDER BY r.dueDate ASC")
    List<ReservationRow> findRowsByStatus(@Param("status") ReservationStatus status);

    /** Echeance proche (alerte vendeur B5) : ACTIVE dont l'echeance <= seuil (ex. +4 jours). */
    @Query("SELECT " + ROW + " FROM Reservation r " +
            "WHERE r.status = :status AND r.dueDate <= :threshold ORDER BY r.dueDate ASC")
    List<ReservationRow> findDueSoon(@Param("status") ReservationStatus status,
                                     @Param("threshold") LocalDateTime threshold);
}
