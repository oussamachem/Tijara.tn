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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    /** Un compte desactive (false) ne peut pas se connecter. */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    /**
     * Tenant (boutique) de l'utilisateur. Sert a poser le contexte tenant apres login (claim JWT +
     * RLS). Nullable cote entite : les profils dev/test (schema create-drop) n'ont pas la contrainte
     * NOT NULL ; en prod la colonne est NOT NULL (Flyway V9). Un futur SUPER_ADMIN plateforme peut
     * ne pas etre rattache a une boutique.
     */
    @Column(name = "boutique_id")
    private Long boutiqueId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
