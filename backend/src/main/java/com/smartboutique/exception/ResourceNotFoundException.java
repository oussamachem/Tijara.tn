package com.smartboutique.exception;

/**
 * Levee quand une ressource demandee n'existe pas (-> HTTP 404).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " introuvable (id=" + id + ")");
    }
}
