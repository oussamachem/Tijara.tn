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

    /** Numero de contact WhatsApp (format international, ex. +21612345678). Public, non sensible :
     *  sert a construire un lien wa.me cote client. Null = pas de contact WhatsApp. */
    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    /** Message WhatsApp par defaut (prefixe du message pre-rempli). Public. Null -> fallback applicatif. */
    @Column(name = "whatsapp_default_message", length = 500)
    private String whatsappDefaultMessage;

    // ---- Transporteur Goodex (Neo Parcel) : identifiants par boutique ----
    /** Token permanent d'authentification Goodex. Jamais renvoye en clair dans les DTO. */
    @Column(name = "goodex_token", length = 1000)
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
