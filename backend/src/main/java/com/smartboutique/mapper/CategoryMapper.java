package com.smartboutique.mapper;

import com.smartboutique.dto.CategoryResponse;
import com.smartboutique.entity.Category;
import org.springframework.stereotype.Component;

/** Conversion entre l'entite Category et ses DTO. */
@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
