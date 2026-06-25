package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Categorie de produits (Homme, Femme, Enfant, ...).
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 500)
    private String description;

    /** Type d'axe taille des produits de cette categorie (definit les tailles autorisees). */
    @Enumerated(EnumType.STRING)
    @Column(name = "size_type", nullable = false, length = 20)
    @Builder.Default
    private SizeType sizeType = SizeType.LETTER;
}
