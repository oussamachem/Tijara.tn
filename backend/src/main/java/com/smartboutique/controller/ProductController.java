package com.smartboutique.controller;

import com.smartboutique.dto.PageResponse;
import com.smartboutique.dto.ProductResponse;
import com.smartboutique.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lecture des produits, accessible a tout utilisateur authentifie (ADMIN et VENDEUR).
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Liste paginee avec recherche/filtre par nom, reference et categorie.
     * Pagination/tri via parametres {@code page}, {@code size}, {@code sort}.
     */
    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return productService.search(name, reference, categoryId, pageable);
    }

    /** Produits en rupture ou sous le seuil d'alerte. */
    @GetMapping("/low-stock")
    public List<ProductResponse> lowStock() {
        return productService.findLowStock();
    }

    /** Recherche d'un produit par le contenu d'un QR Code (la reference) — vente par scan. */
    @GetMapping("/by-qr")
    public ProductResponse byQr(@RequestParam("code") String code) {
        return productService.findByQrContent(code);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return productService.findById(id);
    }

    /** Image PNG du QR Code du produit (pour impression depuis le web). */
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> qrcode(@PathVariable Long id) {
        byte[] png = productService.generateQrPng(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode-" + id + ".png\"")
                .body(png);
    }
}
