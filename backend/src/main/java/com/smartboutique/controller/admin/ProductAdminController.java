package com.smartboutique.controller.admin;

import com.smartboutique.dto.AddVariantRequest;
import com.smartboutique.dto.ImageReorderRequest;
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

import java.util.List;

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

    /** Upload d'une ou plusieurs images produit (multipart/form-data, champ "files"). */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductResponse uploadImages(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) {
        return productService.uploadImages(id, files);
    }

    /** Retire une image de la galerie (ligne DB + fichier disque). */
    @DeleteMapping("/{id}/images/{imageId}")
    public ProductResponse deleteImage(@PathVariable Long id, @PathVariable Long imageId) {
        return productService.deleteImage(id, imageId);
    }

    /** Reordonne la galerie (position 0 = couverture). */
    @PutMapping("/{id}/images/order")
    public ProductResponse reorderImages(@PathVariable Long id, @Valid @RequestBody ImageReorderRequest request) {
        return productService.reorderImages(id, request.imageIds());
    }

    /** Ajoute une variante (declinaison) a un produit existant. */
    @PostMapping("/{id}/variants")
    @ResponseStatus(HttpStatus.CREATED)
    public VariantResponse addVariant(@PathVariable Long id, @Valid @RequestBody AddVariantRequest request) {
        return variantService.addVariant(id, request);
    }
}
