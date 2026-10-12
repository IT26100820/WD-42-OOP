package com.bookstore.review.exception;

/**
 * Base custom runtime exception for the Review and Feedback module.
 * Demonstrates inheritance in OOP exception hierarchy.
 */
public class ReviewException extends RuntimeException {

    public ReviewException(String message) {
        super(message);
    }

    public ReviewException(String message, Throwable cause) {
        super(message, cause);
    }
}
