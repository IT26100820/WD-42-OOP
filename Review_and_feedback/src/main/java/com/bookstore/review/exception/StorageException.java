package com.bookstore.review.exception;

/**
 * Thrown when an I/O or file system operation fails while persisting review data (maps to HTTP 500).
 */
public class StorageException extends ReviewException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
