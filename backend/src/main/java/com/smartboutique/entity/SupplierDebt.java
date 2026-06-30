package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Dette envers un fournisseur (compte a payer). Registre PUREMENT FINANCIER :
 * aucun mouvement de stock, aucun impact CA.
 *
 * <p>Contrairement aux credits, le {@code totalAmount} est SAISI (facture fournisseur), pas
 * derive. Le reste et le statut sont DERIVES (total - somme des paiements).</p>
 *
 * <p>Le lien produit est purement DESCRIPTIF/optionnel : il ne touche jamais l'inventaire.</p>
 */
@Entity
@Table(name = "supplier_debts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierDebt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    /** Montant total de la facture fournisseur (SAISI). */
    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "invoice_reference")
    private String invoiceReference;

    @Column(length = 1000)
    private String description;

    /** Reference DESCRIPTIVE optionnelle a un produit (aucun effet stock). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @OneToMany(mappedBy = "debt", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DebtPayment> payments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void addPayment(DebtPayment payment) {
        payments.add(payment);
        payment.setDebt(this);
    }
}
