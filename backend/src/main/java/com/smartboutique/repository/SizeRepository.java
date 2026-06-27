package com.smartboutique.repository;

import com.smartboutique.entity.Size;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SizeRepository extends JpaRepository<Size, Long> {

    Optional<Size> findByLabelIgnoreCase(String label);

    /** Unicite insensible a la casse (et au trim, applique cote service). */
    boolean existsByLabelIgnoreCase(String label);

    /** Catalogue ordonne : position croissante (nulls en dernier), puis libelle. */
    @Query("SELECT s FROM Size s ORDER BY s.position ASC NULLS LAST, s.label ASC")
    List<Size> findAllOrdered();
}
