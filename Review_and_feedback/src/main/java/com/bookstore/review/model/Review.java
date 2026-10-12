package com.bookstore.review.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Domain model representing a customer's review and rating for a book.
 * Demonstrates Object-Oriented Programming (OOP) concepts:
 * - Encapsulation: Private fields with public accessors and controlled mutation methods.
 * - Invariant Enforcement: Validation ensures an invalid state cannot be represented.
 * - Information Hiding: Internal time management and field modifications are guarded.
 */
public class Review implements Serializable, Comparable<Review> {

    private static final long serialVersionUID = 1L;

    private String reviewId;
    private String bookId;
    private String customerId;
    private int rating;
    private String comment;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * Default constructor required for JSON deserialization (Jackson).
     */
    public Review() {
    }

    /**
     * Parameterized constructor for creating a new review instance.
     *
     * @param reviewId   Unique review identifier (e.g. REV-1001)
     * @param bookId     Identifier of the book being reviewed
     * @param customerId Identifier of the reviewing customer
     * @param rating     Rating value between 1 and 5
     * @param comment    Review text comment
     * @param createdAt  Creation timestamp
     * @param updatedAt  Last modified timestamp
     */
    public Review(String reviewId, String bookId, String customerId, int rating, String comment,
                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        validateRating(rating);
        validateComment(comment);

        this.reviewId = reviewId;
        this.bookId = bookId != null ? bookId.trim() : null;
        this.customerId = customerId != null ? customerId.trim() : null;
        this.rating = rating;
        this.comment = comment != null ? comment.trim() : "";
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    /**
     * Business method to update review content while enforcing encapsulation
     * and automatically maintaining the update timestamp.
     *
     * @param newRating  New rating (1 - 5)
     * @param newComment New comment text
     */
    public void updateContent(int newRating, String newComment) {
        validateRating(newRating);
        validateComment(newComment);
        this.rating = newRating;
        this.comment = newComment.trim();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Validates that the rating falls within the allowed 1-5 inclusive range.
     */
    private void validateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be a whole number between 1 and 5. Given: " + rating);
        }
    }

    /**
     * Validates that the comment is neither null nor empty and does not exceed maximum length.
     */
    private void validateComment(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty or whitespace.");
        }
        if (comment.trim().length() > 1000) {
            throw new IllegalArgumentException("Comment must not exceed 1000 characters.");
        }
    }

    // Getters and Setters

    public String getReviewId() {
        return reviewId;
    }

    public void setReviewId(String reviewId) {
        this.reviewId = reviewId;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId != null ? bookId.trim() : null;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId != null ? customerId.trim() : null;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        validateRating(rating);
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        validateComment(comment);
        this.comment = comment.trim();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public int compareTo(Review other) {
        if (other == null || other.createdAt == null) {
            return 1;
        }
        if (this.createdAt == null) {
            return -1;
        }
        // Default sort: newest first
        return other.createdAt.compareTo(this.createdAt);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Review review = (Review) o;
        return Objects.equals(reviewId, review.reviewId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reviewId);
    }

    @Override
    public String toString() {
        return "Review{" +
                "reviewId='" + reviewId + '\'' +
                ", bookId='" + bookId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", rating=" + rating +
                ", comment='" + (comment != null && comment.length() > 30 ? comment.substring(0, 30) + "..." : comment) + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
