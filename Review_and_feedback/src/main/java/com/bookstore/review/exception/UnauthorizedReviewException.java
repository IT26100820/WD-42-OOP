package com.bookstore.review.exception;

/**
 * Thrown when a customer attempts to modify or delete a review created by another customer (maps to HTTP 403).
 */
public class UnauthorizedReviewException extends ReviewException {

    public UnauthorizedReviewException(String message) {
        super(message);
    }
}
