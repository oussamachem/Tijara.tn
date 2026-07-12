package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Utilisateur de l'application (administrateur ou vendeur).
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    /** Mot de passe chiffre (BCrypt). Jamais expose dans les DTO. */
    @Column(nullable = false)
    private String password;

    /**
     * IDENTITE globale (Phase A) : plus de role global ni de boutique figee sur le user. Le role
     * est CONTEXTUEL (cf. {@link ShopMember} : OWNER/VENDOR d'une boutique). Le seul attribut global
     * d'autorisation est {@code isPlatformAdmin} (ex-SUPER_ADMIN, moderation plateforme).
     */
    @Column(name = "is_platform_admin", nullable = false)
    @Builder.Default
    private boolean platformAdmin = false;

    /** Un compte desactive (false) ne peut pas se connecter. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
