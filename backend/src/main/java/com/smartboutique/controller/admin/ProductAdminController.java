package com.smartboutique.controller.admin;

import com.smartboutique.dto.AddVariantRequest;
import com.smartboutique.dto.ProductHeaderRequest;
import com.smartboutique.dto.ProductRequest;
import com.smartboutique.dto.ProductResponse;
import com.smartboutique.dto.VariantResponse;
import com.smartboutique.service.ProductService;
import com.smartboutique.service.VariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * CRUD des produits (entete + matrice de variantes) + upload d'image, reserve a l'ADMIN.
 * Le QR Code est genere automatiquement par variante a la creation.
 */
@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class ProductAdminController {

    private final ProductService productService;
    private final VariantService variantService;

    /** Creation : entete + liste de variantes (matrice couleur x taille developpee). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return productService.create(request);
    }

    /** Mise a jour de l'entete (les variantes se gerent via les endpoints dedies). */
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductHeaderRequest request) {
        return productService.updateHeader(id, request);
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

    /** Ajoute une variante (declinaison) a un produit existant. */
    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse addVariant(@PathVariable Long id, @Valid @RequestBody AddVariantRequest request) {
        return variantService.addVariant(id, request);
    }
}
