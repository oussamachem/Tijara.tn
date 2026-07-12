package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Boutique = TENANT de la plateforme (SaaS multi-tenant). Registre gere par le SUPER_ADMIN ;
 * cette table n'est PAS elle-meme scopee par tenant (pas de RLS). Le {@code slug} sert a la
 * recherche/aux URLs publiques (marketplace, Phase 4).
 */
@Entity
@Table(name = "boutiques")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Boutique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 140)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BoutiqueStatus status = BoutiqueStatus.ACTIVE;

    /** Proprietaire (user OWNER). Nullable au bootstrap (boutique par defaut avant le seed admin). */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
