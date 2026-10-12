package com.bookstore.review.dto;

import java.util.Map;

/**
 * DTO providing aggregated statistics for a book's reviews:
 * - Average star rating
 * - Total review count
 * - Breakdown by star rating (1 to 5 stars)
 */
public class BookReviewSummary {

    private String bookId;
    private double averageRating;
    private int totalReviews;
    private Map<Integer, Long> ratingDistribution;

    public BookReviewSummary() {
    }

    public BookReviewSummary(String bookId, double averageRating, int totalReviews, Map<Integer, Long> ratingDistribution) {
        this.bookId = bookId;
        this.averageRating = averageRating;
        this.totalReviews = totalReviews;
        this.ratingDistribution = ratingDistribution;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public int getTotalReviews() {
        return totalReviews;
    }

    public void setTotalReviews(int totalReviews) {
        this.totalReviews = totalReviews;
    }

    public Map<Integer, Long> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Long> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }
}
