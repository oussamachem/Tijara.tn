package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Appartenance d'un utilisateur a une boutique avec un role contextuel (OWNER|VENDOR).
 * Table GLOBALE (pas de RLS) : c'est elle qui AUTORISE l'acces a une boutique (validation du
 * header X-Shop-Id). Un user peut etre OWNER de plusieurs boutiques ; un VENDOR d'une seule.
 */
@Entity
@Table(name = "shop_members", uniqueConstraints =
        @UniqueConstraint(name = "uk_shop_members", columnNames = {"shop_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShopMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShopMemberRole role;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
