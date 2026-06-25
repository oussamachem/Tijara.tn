package com.smartboutique.repository;

import com.smartboutique.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByReference(String reference);

    boolean existsByReference(String reference);

    /** Vrai si au moins un produit est rattache a la categorie donnee (garde-fou suppression). */
    boolean existsByCategoryId(Long categoryId);
}
