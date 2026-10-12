package com.bookstore.review.service;

import com.bookstore.review.dto.BookReviewSummary;
import com.bookstore.review.dto.ReviewRequest;
import com.bookstore.review.model.Review;

import java.util.List;

/**
 * Service interface defining business logic operations for reviews.
 * Demonstrates Abstraction:
 * Decouples Web/REST Controllers from persistence details and business validation.
 */
public interface ReviewService {

    /**
     * Retrieves all reviews with optional filtering and sorting.
     *
     * @param bookId   Optional filter by book ID
     * @param rating   Optional filter by rating (1-5)
     * @param search   Optional keyword search in comments
     * @param sortBy   Sorting criteria (newest, oldest, highest_rating, lowest_rating)
     * @return Filtered and sorted list of reviews
     */
    List<Review> getAllReviews(String bookId, Integer rating, String search, String sortBy);

    /**
     * Retrieves a single review by its unique ID.
     *
     * @param reviewId Unique ID
     * @return Found review
     * @throws com.bookstore.review.exception.ResourceNotFoundException if not found
     */
    Review getReviewById(String reviewId);

    /**
     * Retrieves all reviews for a specific book.
     *
     * @param bookId Book ID
     * @return List of reviews
     */
    List<Review> getReviewsByBookId(String bookId);

    /**
     * Calculates review statistics for a book (average rating, review count, rating breakdown).
     *
     * @param bookId Book ID
     * @return Summary statistics
     */
    BookReviewSummary getBookSummary(String bookId);

    /**
     * Creates and persists a new review after enforcing business validation and duplicate checks.
     *
     * @param request Creation request DTO
     * @return The persisted review
     */
    Review createReview(ReviewRequest request);

    /**
     * Updates an existing review's rating and comment.
     * Preserves original ID, book ID, customer ID, and creation timestamp.
     *
     * @param reviewId             ID of review to update
     * @param request              Update request DTO
     * @param requestingCustomerId Customer attempting the update (for permission checking)
     * @return The updated review
     */
    Review updateReview(String reviewId, ReviewRequest request, String requestingCustomerId);

    /**
     * Deletes a review by its ID.
     *
     * @param reviewId             ID of review to delete
     * @param requestingCustomerId Customer attempting the deletion
     */
    void deleteReview(String reviewId, String requestingCustomerId);
}
