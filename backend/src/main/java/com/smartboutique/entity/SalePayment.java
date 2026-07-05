package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Une ligne de paiement (tender) d'une vente : especes, carte ou ticket cadeau.
 * Une vente peut combiner plusieurs tenders (tickets multiples + appoint especes/carte).
 *
 * <p>Le ticket cadeau (cheque habillement Pluxee/Joker) est stocke pour tracabilite : on
 * n'en decode PAS le contenu proprietaire ; {@code ticketCode} = la chaine scannee (QR ou
 * code-barres), unique GLOBALEMENT (usage unique). Le CA reste = prix des articles.</p>
 */
@Entity
@Table(name = "sale_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalePayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TenderMethod method;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;

    // ---- Champs specifiques au ticket cadeau (null pour especes/carte) ----

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TicketIssuer issuer;

    /** Chaine scannee (QR ou code-barres) = identifiant unique global du ticket. */
    @Column(name = "ticket_code", length = 120)
    private String ticketCode;

    /** N° de serie imprime (optionnel). */
    @Column(name = "ticket_serial", length = 80)
    private String ticketSerial;

    /** Date d'expiration imprimee (confirmee/saisie par le vendeur). */
    @Column(name = "ticket_expiry")
    private LocalDate ticketExpiry;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
