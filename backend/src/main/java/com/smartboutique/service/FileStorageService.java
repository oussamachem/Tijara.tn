package com.smartboutique.service;

import com.smartboutique.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Stockage local des images produits.
 *
 * <p>Choix : stockage sur disque dans le dossier {@code uploads/} (configurable via
 * {@code app.uploads-dir}), servi statiquement sous {@code /uploads/**}. Le champ
 * {@code product.image_url} contient l'URL relative (ex. {@code /uploads/<uuid>.png}).
 * En conteneur, ce dossier est monte sur un volume Docker pour la persistance.</p>
 */
@Slf4j
@Service
public class FileStorageService {

    /** Types MIME autorises -> extension de fichier. */
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/webp", "webp");

    private static final long MAX_SIZE_BYTES = 5 * 1024 * 1024; // 5 Mo

    @Value("${app.uploads-dir}")
    private String uploadsDir;

    private Path root;

    @PostConstruct
    void init() {
        try {
            this.root = Paths.get(uploadsDir).toAbsolutePath().normalize();
            Files.createDirectories(root);
            log.info("Dossier d'upload des images : {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de creer le dossier d'upload", e);
        }
    }

    /**
     * Valide un fichier (presence, taille, type) sans l'ecrire. Permet de tout verifier
     * AVANT d'ecrire quoi que ce soit lors d'un upload multiple (pas de fichier orphelin
     * si un fichier du lot est invalide). Renvoie l'extension associee au type MIME.
     */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Aucun fichier fourni", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("Image trop volumineuse (max 5 Mo)", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        String extension = ALLOWED_TYPES.get(contentType == null ? "" : contentType.toLowerCase());
        if (extension == null) {
            throw new BusinessException(
                    "Format d'image non supporte (PNG, JPG ou WEBP attendu)", HttpStatus.BAD_REQUEST);
        }
        return extension;
    }

    /**
     * Enregistre l'image et renvoie son URL relative (servie sous /uploads/**).
     */
    public String store(MultipartFile file) {
        String extension = validate(file);
        String filename = UUID.randomUUID() + "." + extension;
        try {
            Path target = root.resolve(filename).normalize();
            // Securite : empeche toute ecriture hors du dossier d'upload.
            if (!target.getParent().equals(root)) {
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

    /**
     * Supprime du disque le fichier associe a une URL {@code /uploads/<nom>} (anti-orphelins
     * lors du retrait d'une image ou de la suppression d'un produit). Best-effort : ne fait
     * pas echouer l'operation metier si le fichier est deja absent.
     */
    public void delete(String url) {
        if (url == null || !url.startsWith("/uploads/")) {
            return;
        }
        String filename = url.substring("/uploads/".length());
        try {
            Path target = root.resolve(filename).normalize();
            // Securite : ne supprime jamais hors du dossier d'upload (anti path-traversal).
            if (!target.getParent().equals(root)) {
                log.warn("Suppression ignoree (chemin hors uploads) : {}", url);
                return;
            }
            boolean deleted = Files.deleteIfExists(target);
            log.info("Image disque {} : {}", deleted ? "supprimee" : "deja absente", filename);
        } catch (IOException e) {
            // On journalise mais on ne bloque pas l'operation metier (suppression DB deja faite).
            log.warn("Echec suppression fichier image {} : {}", filename, e.getMessage());
        }
    }
}
