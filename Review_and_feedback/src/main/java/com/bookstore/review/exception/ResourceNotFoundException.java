package com.bookstore.review.exception;

/**
 * Thrown when a requested review or resource cannot be found (maps to HTTP 404).
 */
public class ResourceNotFoundException extends ReviewException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
