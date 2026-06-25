package com.smartboutique.controller;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.ProductResponse;
import com.smartboutique.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

/**
 * Lecture des produits (modeles), accessible a tout utilisateur authentifie (ADMIN et VENDEUR).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** Liste paginee avec recherche/filtre par nom, reference et categorie. */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.search(name, reference, categoryId, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.findById(id);
    }
}
