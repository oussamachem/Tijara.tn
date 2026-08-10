package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande en ligne (marketplace). Rattachee a UNE boutique (tenant, RLS) et a un CLIENT global.
 * Le stock n'est PAS decremente a la creation (C3) : il l'est a la confirmation par la boutique
 * (Phase 5). Le total est calcule serveur a partir des prix du tenant du slug.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String reference;

    /** Client (compte global). */
    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.EN_ATTENTE;

    @Column(name = "total_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    // ---- Livraison (snapshot figé à la commande, depuis le profil du client) ----
    @Column(name = "delivery_name", length = 150)
    private String deliveryName;

    @Column(name = "delivery_phone", length = 30)
    private String deliveryPhone;

    @Column(name = "delivery_address", length = 300)
    private String deliveryAddress;

    @Column(name = "delivery_governorat", length = 40)
    private String deliveryGovernorat;

    // ---- Suivi transporteur Goodex ----
    /** Code de suivi (ean) renvoyé par Goodex à la création du colis. Null tant que non expédié. */
    @Column(name = "carrier_ean", length = 60)
    private String carrierEan;

    @Column(name = "carrier_status", length = 60)
    private String carrierStatus;

    @Column(name = "carrier_status_at")
    private LocalDateTime carrierStatusAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
