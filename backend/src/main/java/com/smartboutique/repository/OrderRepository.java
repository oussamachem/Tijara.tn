package com.smartboutique.repository;

import com.smartboutique.entity.Order;
import com.smartboutique.entity.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Commandes. Toutes les requetes passent par la RLS (scope tenant courant) : un boutique-admin ne
 * voit que ses commandes ; un client ne voit ses commandes que dans le contexte d'une boutique.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"items", "items.variant"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findDetailById(@Param("id") Long id);

    /** Commandes du client dans le tenant courant (RLS), plus recentes d'abord. */
    List<Order> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /** Toutes les commandes du tenant courant (espace boutique-admin), lignes chargees. */
    @EntityGraph(attributePaths = {"items"})
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findAllWithItems();

    /** Commandes du tenant filtrees par statut, lignes chargees. */
    @EntityGraph(attributePaths = {"items"})
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);
}
