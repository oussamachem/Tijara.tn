package com.smartboutique.repository;

import com.smartboutique.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    long countByProductId(Long productId);
}
