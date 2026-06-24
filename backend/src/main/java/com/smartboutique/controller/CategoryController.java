package com.smartboutique.controller;

import com.smartboutique.dto.CategoryResponse;
import com.smartboutique.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lecture des categories, accessible a tout utilisateur authentifie
 * (necessaire au web et au mobile pour filtrer/afficher les produits).
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return categoryService.findById(id);
    }
}
