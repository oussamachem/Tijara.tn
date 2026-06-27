package com.smartboutique.mapper;

import com.smartboutique.dto.ProductImageResponse;
import com.smartboutique.dto.ProductResponse;
import com.smartboutique.dto.VariantResponse;
import com.smartboutique.entity.Category;
import com.smartboutique.entity.Product;
import com.smartboutique.entity.ProductImage;
import com.smartboutique.entity.ProductVariant;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/** Conversion entre Product/ProductVariant et leurs DTO. */
@Component
public class ProductMapper {

    public boolean isLowStock(ProductVariant v) {
        return v.getQuantity() != null && v.getSeuilAlerte() != null
                && v.getQuantity() <= v.getSeuilAlerte();
    }

    public VariantResponse toVariantResponse(ProductVariant v) {
        return new VariantResponse(
                v.getId(),
                v.getColor() != null ? v.getColor().getId() : null,
                v.getColor() != null ? v.getColor().getName() : null,
                v.getColor() != null ? v.getColor().getHex() : null,
                v.getSize().getLabel(),
                v.getQuantity(),
                v.getSeuilAlerte(),
                isLowStock(v),
                v.getReference(),
                v.getQrCode()
        );
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        List<ProductVariant> variants = product.getVariants();

        List<VariantResponse> variantDtos = variants.stream()
                .sorted(Comparator.comparing(ProductVariant::getReference))
                .map(this::toVariantResponse)
                .toList();

        int totalStock = variants.stream().mapToInt(v -> v.getQuantity() == null ? 0 : v.getQuantity()).sum();
        boolean anyLow = variants.stream().anyMatch(this::isLowStock);

        // Galerie triee par position ; la couverture (position 0) reste exposee en imageUrl.
        List<ProductImageResponse> images = product.getImages().stream()
                .sorted(Comparator.comparingInt(ProductImage::getPosition))
                .map(img -> new ProductImageResponse(img.getId(), img.getUrl(), img.getPosition()))
                .toList();
        String coverUrl = images.isEmpty() ? null : images.get(0).url();

        return new ProductResponse(
                product.getId(),
                product.getReference(),
                product.getName(),
                product.getDescription(),
                category != null ? category.getId() : null,
                category != null ? category.getName() : null,
                product.getPurchasePrice(),
                product.getSalePrice(),
                coverUrl,
                images,
                product.getCreatedAt(),
                totalStock,
                anyLow,
                variantDtos
        );
    }
}
