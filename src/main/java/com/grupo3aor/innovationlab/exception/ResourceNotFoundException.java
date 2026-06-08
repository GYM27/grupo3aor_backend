package com.grupo3aor.innovationlab.exception;

/**
 * Custom exception used when a requested resource is not found in the database.
 * This exception will be intercepted by the GlobalExceptionHandler to return a 404 status.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
