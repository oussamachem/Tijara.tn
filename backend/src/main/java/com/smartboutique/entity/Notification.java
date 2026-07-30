package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notification in-app d'un utilisateur (identite). Table GLOBALE (pas de tenant / RLS) : la
 * cible est un {@code user_id}. Aujourd'hui alimentee par les changements de statut de commande.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 400)
    private String body;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
