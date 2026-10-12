package com.bookstore.review.repository;

import com.bookstore.review.model.Review;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object (DAO) interface defining CRUD contracts for Review entities.
 * Demonstrates OOP Abstraction:
 * Higher-level services depend on this abstract interface rather than a concrete storage mechanism,
 * allowing file storage to be swapped for a relational database (e.g. MySQL, PostgreSQL, H2) with zero service changes.
 */
public interface ReviewRepository {

    /**
     * Retrieves all saved reviews.
     *
     * @return List of reviews
     */
    List<Review> findAll();

    /**
     * Finds a review by its unique identifier.
     *
     * @param reviewId Unique ID
     * @return Optional containing the review if found
     */
    Optional<Review> findById(String reviewId);

    /**
     * Retrieves all reviews associated with a specific book.
     *
     * @param bookId Book identifier
     * @return List of matching reviews
     */
    List<Review> findByBookId(String bookId);

    /**
     * Retrieves all reviews submitted by a specific customer.
     *
     * @param customerId Customer identifier
     * @return List of matching reviews
     */
    List<Review> findByCustomerId(String customerId);

    /**
     * Checks if a review with the specified ID exists.
     *
     * @param reviewId Unique ID
     * @return true if exists, false otherwise
     */
    boolean existsById(String reviewId);

    /**
     * Checks if a specific customer has already submitted a review for a specific book.
     * Used to enforce duplicate prevention.
     *
     * @param bookId     Book identifier
     * @param customerId Customer identifier
     * @return true if already reviewed, false otherwise
     */
    boolean existsByBookIdAndCustomerId(String bookId, String customerId);

    /**
     * Saves a review (either new or existing update) into persistent storage.
     *
     * @param review Review to persist
     * @return The persisted review with assigned ID
     */
    Review save(Review review);

    /**
     * Deletes a review by its unique ID.
     *
     * @param reviewId ID of the review to remove
     * @return true if deleted, false if not found
     */
    boolean deleteById(String reviewId);

    /**
     * Returns total count of reviews.
     *
     * @return Count
     */
    long count();

    /**
     * Removes all reviews from storage (used primarily for test cleanup).
     */
    void deleteAll();
}
