package com.smartboutique.repository;

import com.smartboutique.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ColorRepository extends JpaRepository<Color, Long> {

    Optional<Color> findByName(String name);

    boolean existsByName(String name);
}
