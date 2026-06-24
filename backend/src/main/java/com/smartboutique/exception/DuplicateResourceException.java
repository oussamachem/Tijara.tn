package com.smartboutique.exception;

/**
 * Levee lors d'un conflit (ressource deja existante : email, reference...) -> HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
