package com.smartboutique.controller;

import com.smartboutique.dto.VariantScanResponse;
import com.smartboutique.service.VariantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Lecture au grain VARIANTE, accessible a tout utilisateur authentifie (ADMIN et VENDEUR) :
 * resolution scan, alertes de stock, image QR.
 */
@RestController
@RequestMapping("/api/variants")
@RequiredArgsConstructor
public class VariantController {

    private final VariantService variantService;

    /** Resolution scan : contenu du QR (= reference variante) -> variante + infos produit. */
    @GetMapping("/by-qr")
    public VariantScanResponse byQr(@RequestParam("code") String code) {
        return variantService.findByQrContent(code);
    }

    /** Variantes en rupture ou sous le seuil d'alerte. */
    @GetMapping("/low-stock")
    public List<VariantScanResponse> lowStock() {
        return variantService.findLowStock();
    }

    /** Image PNG du QR Code d'une variante (impression etiquette). */
    @GetMapping("/{id}/qrcode")
    public ResponseEntity<byte[]> qrcode(@PathVariable Long id) {
        byte[] png = variantService.generateQrPng(id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"qrcode-variant-" + id + ".png\"")
                .body(png);
    }
}
