package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token de reinitialisation de mot de passe a usage unique et a duree limitee.
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Date d'expiration du token. */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Indique si le token a deja servi (usage unique). */
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Vrai si le token est expire ou deja utilise. */
    @Transient
    public boolean isValidNow() {
        return !used && expiresAt.isAfter(LocalDateTime.now());
    }
}
