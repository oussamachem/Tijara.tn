package com.smartboutique.service;

import com.smartboutique.dto.CategoryRequest;
import com.smartboutique.dto.CategoryResponse;
import com.smartboutique.dto.SizeOptionsResponse;
import com.smartboutique.entity.Category;
import com.smartboutique.exception.BusinessException;
import com.smartboutique.exception.DuplicateResourceException;
import com.smartboutique.exception.ResourceNotFoundException;
import com.smartboutique.mapper.CategoryMapper;
import com.smartboutique.repository.CategoryRepository;
import com.smartboutique.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion des categories de produits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll().stream().map(categoryMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return categoryMapper.toResponse(getCategory(id));
    }

    /** Tailles autorisees pour la categorie (alimente le formulaire de creation web). */
    @Transactional(readOnly = true)
    public SizeOptionsResponse sizeOptions(Long id) {
        Category category = getCategory(id);
        return new SizeOptionsResponse(category.getSizeType(), category.getSizeType().allowedSizes());
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Une categorie portant ce nom existe deja");
        }
        Category category = Category.builder()
                .name(request.name())
                .description(request.description())
                .sizeType(request.sizeType())
                .build();
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = getCategory(id);
        // Verifie l'unicite du nom si modifie.
        if (!category.getName().equalsIgnoreCase(request.name())
                && categoryRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Une categorie portant ce nom existe deja");
        }
        category.setName(request.name());
        category.setDescription(request.description());
        category.setSizeType(request.sizeType());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    /** Supprime une categorie, sauf si des produits y sont encore rattaches (409). */
    @Transactional
    public void delete(Long id) {
        Category category = getCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException(
                    "Impossible de supprimer cette categorie : des produits y sont rattaches",
                    HttpStatus.CONFLICT);
        }
        categoryRepository.delete(category);
        log.info("Categorie supprimee : {} (id={})", category.getName(), id);
    }

    private Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categorie", id));
    }
}
