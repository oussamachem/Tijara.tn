package com.smartboutique.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Couleur du catalogue (gere par l'ADMIN, comme les categories).
 */
@Entity
@Table(name = "colors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Color {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    /** Code hexadecimal optionnel (ex. #1E40AF) pour l'affichage. */
    @Column(length = 7)
    private String hex;
}
