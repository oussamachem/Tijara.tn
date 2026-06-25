package com.smartboutique.repository.specification;

import com.smartboutique.entity.Product;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Criteres de recherche dynamiques pour les produits (utilises avec JpaSpecificationExecutor).
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Recherche insensible a la casse sur le nom. */
    public static Specification<Product> nameContains(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String pattern = "%" + name.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
    }

    /** Recherche insensible a la casse sur la reference. */
    public static Specification<Product> referenceContains(String reference) {
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        String pattern = "%" + reference.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("reference")), pattern);
    }

    /** Filtre par categorie. */
    public static Specification<Product> hasCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }
}
