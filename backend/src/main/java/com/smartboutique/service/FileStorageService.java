package com.smartboutique.service;

import com.smartboutique.exception.BusinessException;
import com.smartboutique.service.storage.ImageStorage;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stockage LOCAL des images (disque, dossier {@code app.uploads-dir}, monté sur un volume Docker).
 * Implémentation par défaut de {@link ImageStorage} — active sauf si {@code app.storage=s3}.
 * Le champ {@code image_url} contient l'URL relative {@code /uploads/<uuid>.<ext>}.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage", havingValue = "local", matchIfMissing = true)
public class FileStorageService implements ImageStorage {

    @Value("${app.uploads-dir}")
    private String uploadsDir;

    private Path root;

    @PostConstruct
    void init() {
        try {
            this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            log.info("[storage=local] Dossier d'upload des images : {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer le dossier d'upload", e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        String extension = validate(file);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Path target = root.resolve(filename).normalize();
            if (!target.getParent().equals(root)) {   // anti path-traversal
                throw new BusinessException("Chemin de fichier invalide", HttpStatus.BAD_REQUEST);
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Echec de l'enregistrement de l'image", e);
        }
        log.info("Image enregistree : {} ({})", filename, StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename()));
        return "/uploads/" + filename;
    }

    @Override
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) return;
        String filename = url.substring("/uploads/".length());
        try {
            Path target = root.resolve(filename).normalize();
            if (!target.getParent().equals(root)) {   // anti path-traversal
                log.warn("Suppression ignoree (chemin hors uploads) : {}", url);
                return;
            }
            boolean deleted = Files.deleteIfExists(target);
            log.info("Image disque {} : {}", deleted ? "supprimee" : "deja absente", filename);
        } catch (IOException e) {
            log.warn("Echec suppression fichier image {} : {}", filename, e.getMessage());
        }
    }

    @Override
    public LoadedImage load(String filename) {
        try {
            Path target = root.resolve(filename).normalize();
            if (!target.getParent().equals(root) || !Files.isReadable(target)) return null;  // anti-traversal + absence
            return new LoadedImage(Files.readAllBytes(target), ImageStorage.contentTypeFor(filename));
        } catch (IOException e) {
            log.warn("Echec lecture image {} : {}", filename, e.getMessage());
            return null;
        }
    }
}
