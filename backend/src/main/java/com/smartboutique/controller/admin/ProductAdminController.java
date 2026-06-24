package com.smartboutique.controller.admin;

import com.smartboutique.dto.ProductRequest;
import com.smartboutique.dto.ProductResponse;
import com.smartboutique.dto.StockAdjustRequest;
import com.smartboutique.dto.StockSetRequest;
import com.smartboutique.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * CRUD des produits + upload d'image, reserve a l'ADMIN (route sous /api/admin/**).
 * Le QR Code est genere automatiquement a la creation.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
    }

    /** Upload d'une image produit (multipart/form-data, champ "file"). */
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return productService.uploadImage(id, file);
    }

    /** Definit la quantite absolue en stock (inventaire). */
    @PatchMapping("/{id}/stock")
    public ProductResponse setStock(@PathVariable Long id, @Valid @RequestBody StockSetRequest request) {
        return productService.setStock(id, request.quantity());
    }

    /** Ajuste le stock d'une valeur relative (entree/sortie manuelle). */
    @PatchMapping("/{id}/stock/adjust")
    public ProductResponse adjustStock(@PathVariable Long id, @Valid @RequestBody StockAdjustRequest request) {
        return productService.adjustStock(id, request.delta());
    }
}
