package com.bookstore.review.exception;

/**
 * Thrown when a customer attempts to submit a second review for the same book (maps to HTTP 409).
 */
public class DuplicateReviewException extends ReviewException {

    public DuplicateReviewException(String message) {
        super(message);
    }
}
