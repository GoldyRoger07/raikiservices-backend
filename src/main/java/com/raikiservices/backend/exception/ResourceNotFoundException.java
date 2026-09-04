package com.raikiservices.backend.exception;

/** Ressource demandée inexistante — traduite en 404 par le GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException(resource + " introuvable : " + id);
    }
}
