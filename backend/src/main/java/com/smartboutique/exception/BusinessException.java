package com.smartboutique.exception;

import org.springframework.http.HttpStatus;

/**
 * Erreur de regle metier avec code HTTP personnalisable
 * (ex. stock insuffisant -> 409). Utilisee a partir de la Phase 4.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    public BusinessException(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
