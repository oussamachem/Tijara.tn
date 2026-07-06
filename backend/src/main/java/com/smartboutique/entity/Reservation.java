package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reservation (layaway) : le produit est RETENU en boutique (stock decremente des la creation)
 * contre un ou plusieurs versements. Le client le recupere une fois SOLDE.
 *
 * <p><b>Total</b> = somme des lignes (prix captures a la creation). <b>Reste</b> et <b>statut de
 * paiement</b> sont DERIVES (total - somme des versements) : aucune colonne d'argent qui diverge.</p>
 *
 * <p><b>CA</b> = reconnu <b>a la cloture</b> (B3) : la vente {@link Sale} de reconnaissance n'est
 * creee qu'au passage COMPLETED, sans re-decrementer le stock (deja retenu). {@link #sale} lie la
 * reservation a cette vente.</p>
 */
@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Reference lisible unique (ex. RES-000042), attribuee apres l'insert. */
    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "customer_phone", length = 40)
    private String customerPhone;

    /** Vendeur ayant cree la reservation. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReservationStatus status = ReservationStatus.ACTIVE;

    /** Total du a la reservation (somme des lignes, prix captures). */
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    /** Acompte retenu (B6) : positionne a l'expiration/annulation si un versement avait ete fait. */
    @Column(name = "deposit_forfeited", nullable = false)
    @Builder.Default
    private boolean depositForfeited = false;

    /** Vente de reconnaissance du CA (creee a COMPLETED, stock deja retenu -> non re-decremente). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id")
    private Sale sale;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Echeance : passe cette date sans solder -> expiration (stock rendu). */
    @Column(name = "due_date", nullable = false)
    private LocalDateTime dueDate;

    /** Date de cloture (COMPLETED/EXPIRED/CANCELLED). */
    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReservationPayment> payments = new ArrayList<>();

    public void addItem(ReservationItem item) {
        items.add(item);
        item.setReservation(this);
    }

    public void addPayment(ReservationPayment payment) {
        payments.add(payment);
        payment.setReservation(this);
    }
}
