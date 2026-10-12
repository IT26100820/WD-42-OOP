package com.bookstore.review.service;

import com.bookstore.review.dto.BookReviewSummary;
import com.bookstore.review.dto.ReviewRequest;
import com.bookstore.review.exception.DuplicateReviewException;
import com.bookstore.review.exception.ResourceNotFoundException;
import com.bookstore.review.exception.UnauthorizedReviewException;
import com.bookstore.review.model.Review;
import com.bookstore.review.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the ReviewService.
 * Enforces business rules:
 * - Duplicate review prevention (one review per customer per book).
 * - Ownership/authorization verification before update or deletion.
 * - Input trimming and boundary checks.
 * - Dynamic filtering, keyword searching, and multiple sorting strategies.
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final boolean enforceOneReviewPerCustomer;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             @Value("${app.rules.one-review-per-customer:true}") boolean enforceOneReviewPerCustomer) {
        this.reviewRepository = reviewRepository;
        this.enforceOneReviewPerCustomer = enforceOneReviewPerCustomer;
    }

    @Override
    public List<Review> getAllReviews(String bookId, Integer rating, String search, String sortBy) {
        List<Review> reviews = reviewRepository.findAll();

        // Filter by bookId if specified
        if (bookId != null && !bookId.trim().isEmpty()) {
            String targetBook = bookId.trim();
            reviews = reviews.stream()
                    .filter(r -> targetBook.equalsIgnoreCase(r.getBookId()))
                    .collect(Collectors.toList());
        }

        // Filter by rating if specified
        if (rating != null) {
            reviews = reviews.stream()
                    .filter(r -> r.getRating() == rating)
                    .collect(Collectors.toList());
        }

        // Keyword search in comments (case-insensitive)
        if (search != null && !search.trim().isEmpty()) {
            String keyword = search.trim().toLowerCase();
            reviews = reviews.stream()
                    .filter(r -> r.getComment() != null && r.getComment().toLowerCase().contains(keyword))
                    .collect(Collectors.toList());
        }

        // Sort reviews based on criteria
        Comparator<Review> comparator;
        if ("oldest".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
        } else if ("highest_rating".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt(Review::getRating).reversed()
                    .thenComparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        } else if ("lowest_rating".equalsIgnoreCase(sortBy)) {
            comparator = Comparator.comparingInt(Review::getRating)
                    .thenComparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        } else {
            // Default: newest first
            comparator = Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));
        }

        return reviews.stream().sorted(comparator).collect(Collectors.toList());
    }

    @Override
    public Review getReviewById(String reviewId) {
        if (reviewId == null || reviewId.trim().isEmpty()) {
            throw new IllegalArgumentException("Review ID cannot be null or empty.");
        }

        return reviewRepository.findById(reviewId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Review not found with ID: " + reviewId.trim()));
    }

    @Override
    public List<Review> getReviewsByBookId(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Book ID cannot be null or empty.");
        }

        return reviewRepository.findByBookId(bookId.trim()).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Override
    public BookReviewSummary getBookSummary(String bookId) {
        if (bookId == null || bookId.trim().isEmpty()) {
            throw new IllegalArgumentException("Book ID cannot be null or empty.");
        }

        List<Review> bookReviews = reviewRepository.findByBookId(bookId.trim());

        Map<Integer, Long> distribution = new LinkedHashMap<>();
        for (int star = 5; star >= 1; star--) {
            final int s = star;
            long count = bookReviews.stream().filter(r -> r.getRating() == s).count();
            distribution.put(star, count);
        }

        int totalReviews = bookReviews.size();
        double averageRating = 0.0;
        if (totalReviews > 0) {
            double sum = bookReviews.stream().mapToInt(Review::getRating).sum();
            double avg = sum / totalReviews;
            averageRating = BigDecimal.valueOf(avg)
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new BookReviewSummary(bookId.trim(), averageRating, totalReviews, distribution);
    }

    @Override
    public Review createReview(ReviewRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Review request cannot be null.");
        }

        String bookId = request.getBookId() != null ? request.getBookId().trim() : "";
        String customerId = request.getCustomerId() != null ? request.getCustomerId().trim() : "";
        String comment = request.getComment() != null ? request.getComment().trim() : "";
        int rating = request.getRating() != null ? request.getRating() : 0;

        // Duplicate check rule
        if (enforceOneReviewPerCustomer && reviewRepository.existsByBookIdAndCustomerId(bookId, customerId)) {
            logger.warn("Duplicate review rejected: customer {} has already reviewed book {}", customerId, bookId);
            throw new DuplicateReviewException("Customer '" + customerId + "' has already submitted a review for book '" + bookId + "'. Only one review per book is allowed.");
        }

        LocalDateTime now = LocalDateTime.now();
        Review review = new Review(null, bookId, customerId, rating, comment, now, now);

        Review saved = reviewRepository.save(review);
        logger.info("Successfully created review {} for book {} by customer {}", saved.getReviewId(), bookId, customerId);
        return saved;
    }

    @Override
    public Review updateReview(String reviewId, ReviewRequest request, String requestingCustomerId) {
        if (reviewId == null || reviewId.trim().isEmpty()) {
            throw new IllegalArgumentException("Review ID cannot be null or empty.");
        }
        if (request == null) {
            throw new IllegalArgumentException("Review request cannot be null.");
        }

        Review existingReview = getReviewById(reviewId.trim());

        // Permission check: only the author or admin can update
        if (requestingCustomerId != null && !requestingCustomerId.trim().isEmpty() &&
                !"admin".equalsIgnoreCase(requestingCustomerId.trim()) &&
                !existingReview.getCustomerId().equalsIgnoreCase(requestingCustomerId.trim())) {
            logger.warn("Unauthorized update: customer {} tried to edit review owned by {}", requestingCustomerId, existingReview.getCustomerId());
            throw new UnauthorizedReviewException("You do not have permission to edit this review. It was submitted by customer '" + existingReview.getCustomerId() + "'.");
        }

        // Update rating and comment, preserving ID, bookId, customerId, and createdAt
        existingReview.updateContent(request.getRating(), request.getComment());

        Review updated = reviewRepository.save(existingReview);
        logger.info("Successfully updated review {}", reviewId);
        return updated;
    }

    @Override
    public void deleteReview(String reviewId, String requestingCustomerId) {
        if (reviewId == null || reviewId.trim().isEmpty()) {
            throw new IllegalArgumentException("Review ID cannot be null or empty.");
        }

        Review existingReview = getReviewById(reviewId.trim());

        // Permission check
        if (requestingCustomerId != null && !requestingCustomerId.trim().isEmpty() &&
                !"admin".equalsIgnoreCase(requestingCustomerId.trim()) &&
                !existingReview.getCustomerId().equalsIgnoreCase(requestingCustomerId.trim())) {
            logger.warn("Unauthorized delete: customer {} tried to delete review owned by {}", requestingCustomerId, existingReview.getCustomerId());
            throw new UnauthorizedReviewException("You do not have permission to delete this review. It was submitted by customer '" + existingReview.getCustomerId() + "'.");
        }

        boolean deleted = reviewRepository.deleteById(reviewId.trim());
        if (!deleted) {
            throw new ResourceNotFoundException("Review not found with ID: " + reviewId.trim());
        }
        logger.info("Successfully deleted review {}", reviewId);
    }
}
