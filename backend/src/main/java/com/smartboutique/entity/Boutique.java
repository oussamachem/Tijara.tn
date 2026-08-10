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

    /** Logo / photo de profil (URL relative /uploads/...), affiche sur la vitrine et le marketplace. */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    // ---- Transporteur Goodex (Neo Parcel) : identifiants par boutique ----
    /** Token permanent d'authentification Goodex. Jamais renvoye en clair dans les DTO. */
    @Column(name = "goodex_token", length = 255)
    private String goodexToken;

    /** Identifiant expediteur (user_id) chez Goodex. */
    @Column(name = "goodex_user_id", length = 60)
    private String goodexUserId;

    /** Base URL de l'API (defaut applique cote service si vide). */
    @Column(name = "goodex_base_url", length = 200)
    private String goodexBaseUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
