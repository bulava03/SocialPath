package com.socialpath.exception;

/**
 * Thrown when a requested user, group, or other entity does not exist.
 * MVC handlers turn this into a redirect; REST handlers turn it into an
 * error code.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
