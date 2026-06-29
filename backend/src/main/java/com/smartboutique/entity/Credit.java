package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Vente A CREDIT : un credit est une {@link Sale} (1-1) augmentee d'un client, d'une echeance
 * et d'un plan de paiement ({@link CreditPayment}).
 *
 * <p>Le TOTAL vient de la vente (somme des lignes, prix captures) ; le RESTE et le STATUT sont
 * DERIVES (total - somme des paiements). Aucune colonne d'argent qui pourrait diverger.</p>
 *
 * <p>Reconnaissance du CA = <b>accrual</b> : la vente compte pour son total le jour de la vente
 * (dashboard ventes inchange). L'impaye est suivi dans le dashboard credits (tresorerie).</p>
 */
@Entity
@Table(name = "credits", uniqueConstraints = @UniqueConstraint(name = "uk_credits_sale", columnNames = "sale_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Credit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vente sous-jacente (stock deja decremente, lignes = produits vendus). 1-1. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** Echeance de paiement (optionnelle). */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /** Annulation tracable (restock + reversal) : le credit reste visible mais neutralise. */
    @Column(nullable = false)
    @Builder.Default
    private boolean cancelled = false;

    @OneToMany(mappedBy = "credit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CreditPayment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void addPayment(CreditPayment payment) {
        payments.add(payment);
        payment.setCredit(this);
    }
}
