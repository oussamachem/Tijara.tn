package com.smartboutique.repository;

import com.smartboutique.entity.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {

    Optional<Boutique> findBySlug(String slug);

    boolean existsBySlug(String slug);
}
