package com.smartboutique.controller;

import com.smartboutique.service.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Sert les images sous {@code /uploads/<nom>} — PUBLIC. Délègue au {@link ImageStorage} actif
 * (disque local OU S3/MinIO) : l'URL est identique quel que soit le backend de stockage.
 */
@RestController
@RequiredArgsConstructor
public class UploadsController {

    private final ImageStorage storage;

    @GetMapping("/uploads/{filename:.+}")
    public ResponseEntity<byte[]> serve(@PathVariable String filename) {
        ImageStorage.LoadedImage img = storage.load(filename);
        if (img == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, img.contentType())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePublic())
                .body(img.bytes());
    }
}
