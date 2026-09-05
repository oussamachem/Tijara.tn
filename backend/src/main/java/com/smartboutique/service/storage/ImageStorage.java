package com.smartboutique.service.storage;

import com.smartboutique.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Abstraction du stockage des images (logos boutique + photos produits). Deux implémentations,
 * choisies par la propriété {@code app.storage} :
 * <ul>
 *   <li>{@code local} (défaut) : disque + volume Docker ({@link com.smartboutique.service.FileStorageService});</li>
 *   <li>{@code s3} : objet S3 via le SDK AWS ({@link S3ImageStorage}) — vrai AWS S3 ou MinIO auto-hébergé.</li>
 * </ul>
 * Dans les deux cas, {@code product.image_url} reste {@code /uploads/<uuid>.<ext>} : le front ne change pas.
 */
public interface ImageStorage {

    /** Types MIME autorisés -> extension. */
    Map<String, String> ALLOWED_TYPES = Map.of(
            "image/png", "png",
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/webp", "webp");

    long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 Mo

    /** Valide (présence, taille, type) SANS écrire ; renvoie l'extension. Partagé par les deux impl. */
    default String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Aucun fichier fourni", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BusinessException("Image trop volumineuse (max 5 Mo)", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        String extension = ALLOWED_TYPES.get(contentType == null ? "" : contentType.toLowerCase());
        if (extension == null) {
            throw new BusinessException("Format d'image non supporté (PNG, JPG ou WEBP attendu)", HttpStatus.BAD_REQUEST);
        }
        return extension;
    }

    /** Enregistre l'image, renvoie son URL relative {@code /uploads/<uuid>.<ext>}. */
    String store(MultipartFile file);

    /** Supprime l'image désignée par une URL {@code /uploads/<nom>} (best-effort). */
    void delete(String url);

    /** Charge le contenu d'un fichier {@code <nom>} pour le servir sous {@code /uploads/<nom>} ; null si absent. */
    LoadedImage load(String filename);

    /** Content type MIME déduit d'une extension de fichier. */
    static String contentTypeFor(String filename) {
        String f = filename == null ? "" : filename.toLowerCase();
        if (f.endsWith(".png")) return "image/png";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    /** Image chargée : octets + type MIME. */
    record LoadedImage(byte[] bytes, String contentType) {}
}
