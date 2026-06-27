package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Taille gere du catalogue (S, M, L, 38, 40, ...). Reference par les produits
 * via une FK (comme {@link Category}), plutot qu'une chaine en texte libre.
 */
@Entity
@Table(name = "sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Size {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Libelle affiche. Unicite (insensible a la casse) verifiee au niveau service. */
    @Column(nullable = false, unique = true)
    private String label;

    /** Ordre d'affichage dans les menus (optionnel) : tries position croissante puis libelle. */
    @Column(name = "position")
    private Integer position;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
