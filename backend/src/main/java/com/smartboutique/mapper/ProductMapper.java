package com.smartboutique.mapper;

import com.smartboutique.dto.ProductResponse;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.Product;
import org.springframework.stereotype.Component;

/** Conversion entre l'entite Product et ses DTO. */
@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        boolean lowStock = product.getQuantity() != null
                && product.getSeuilAlerte() != null
                && product.getQuantity() <= product.getSeuilAlerte();

        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getDescription(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                product.getSize(),
                product.getColor(),
                product.getPurchasePrice(),
                product.getSalePrice(),
                product.getQuantity(),
                product.getSeuilAlerte(),
                lowStock,
                product.getImageUrl(),
                product.getQrCode(),
                product.getCreatedAt()
        );
    }
}
