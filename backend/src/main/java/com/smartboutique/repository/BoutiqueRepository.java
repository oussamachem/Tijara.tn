package com.smartboutique.repository;

import com.smartboutique.entity.Boutique;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BoutiqueRepository extends JpaRepository<Boutique, Long> {

    Optional<Boutique> findBySlug(String slug);

    boolean existsBySlug(String slug);

    /** Annuaire public : boutiques ACTIVES dont le nom ou le slug contient la recherche. */
    @Query("SELECT b FROM Boutique b WHERE b.status = com.smartboutique.entity.BoutiqueStatus.ACTIVE "
            + "AND (LOWER(b.name) LIKE LOWER(CONCAT('%', :q, '%')) "
            + "OR LOWER(b.slug) LIKE LOWER(CONCAT('%', :q, '%'))) ORDER BY b.name")
    List<Boutique> searchActive(@Param("q") String q);
}
