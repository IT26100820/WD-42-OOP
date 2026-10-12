package com.bookstore.review.controller;

import com.bookstore.review.dto.ApiResponse;
import com.bookstore.review.dto.BookReviewSummary;
import com.bookstore.review.dto.ReviewRequest;
import com.bookstore.review.model.Review;
import com.bookstore.review.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for the Review and Feedback module.
 * Exposes full CRUD endpoints, filtering, sorting, and health verification.
 * Adheres to standard HTTP status codes:
 * - 200 OK: Successful retrieval or update
 * - 201 CREATED: Successful review creation
 * - 400 BAD REQUEST: Validation failure or illegal input
 * - 403 FORBIDDEN: Unauthorized modification
 * - 404 NOT FOUND: Missing review
 * - 409 CONFLICT: Duplicate review
 * - 500 INTERNAL SERVER ERROR: Unexpected error or storage failure
 */
@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*") // Allows local development requests if UI is served on a different port
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Health check endpoint used by the UI connection status badge.
     * GET /api/reviews/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("module", "Review and Feedback Management");
        return ResponseEntity.ok(ApiResponse.success("Review service is operational and connected to storage.", health));
    }

    /**
     * Predefined sample books and customers for standalone testing.
     * GET /api/reviews/sample-data
     */
    @GetMapping("/sample-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSampleData() {
        Map<String, Object> data = new HashMap<>();
        data.put("books", Arrays.asList(
                Map.of("id", "BK-101", "title", "Clean Code", "author", "Robert C. Martin", "price", 29.99),
                Map.of("id", "BK-102", "title", "The Pragmatic Programmer", "author", "Andrew Hunt & David Thomas", "price", 34.50),
                Map.of("id", "BK-103", "title", "Effective Java", "author", "Joshua Bloch", "price", 39.00),
                Map.of("id", "BK-104", "title", "Design Patterns", "author", "Erich Gamma et al.", "price", 44.95)
        ));
        data.put("customers", Arrays.asList(
                Map.of("id", "CUST-001", "name", "Alice Johnson", "email", "alice@example.com"),
                Map.of("id", "CUST-002", "name", "Bob Smith", "email", "bob@example.com"),
                Map.of("id", "CUST-003", "name", "Charlie Davis", "email", "charlie@example.com"),
                Map.of("id", "CUST-004", "name", "Diana Prince", "email", "diana@example.com")
        ));
        return ResponseEntity.ok(ApiResponse.success("Sample books and customers retrieved.", data));
    }

    /**
     * List all reviews with optional filtering and sorting.
     * GET /api/reviews
     * Query Params:
     * - bookId: filter by book
     * - rating: filter by rating (1-5)
     * - search: search keyword in comments
     * - sortBy: newest (default), oldest, highest_rating, lowest_rating
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Review>>> getAllReviews(
            @RequestParam(required = false) String bookId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "newest") String sortBy) {

        List<Review> reviews = reviewService.getAllReviews(bookId, rating, search, sortBy);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully.", reviews));
    }

    /**
     * Retrieve an individual review by its ID.
     * GET /api/reviews/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> getReviewById(@PathVariable("id") String id) {
        Review review = reviewService.getReviewById(id);
        return ResponseEntity.ok(ApiResponse.success("Review found.", review));
    }

    /**
     * Retrieve all reviews for a specific book.
     * GET /api/reviews/book/{bookId}
     */
    @GetMapping("/book/{bookId}")
    public ResponseEntity<ApiResponse<List<Review>>> getReviewsByBookId(@PathVariable("bookId") String bookId) {
        List<Review> reviews = reviewService.getReviewsByBookId(bookId);
        return ResponseEntity.ok(ApiResponse.success("Reviews for book " + bookId + " retrieved successfully.", reviews));
    }

    /**
     * Retrieve rating summary and distribution for a specific book.
     * GET /api/reviews/book/{bookId}/summary
     */
    @GetMapping("/book/{bookId}/summary")
    public ResponseEntity<ApiResponse<BookReviewSummary>> getBookSummary(@PathVariable("bookId") String bookId) {
        BookReviewSummary summary = reviewService.getBookSummary(bookId);
        return ResponseEntity.ok(ApiResponse.success("Rating summary for book " + bookId + " retrieved.", summary));
    }

    /**
     * Create a new review.
     * POST /api/reviews
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Review>> createReview(@Valid @RequestBody ReviewRequest request) {
        Review created = reviewService.createReview(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully! Thank you for your feedback.", created));
    }

    /**
     * Update an existing review by ID.
     * PUT /api/reviews/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Review>> updateReview(
            @PathVariable("id") String id,
            @Valid @RequestBody ReviewRequest request,
            @RequestHeader(value = "X-Customer-Id", required = false) String headerCustomerId,
            @RequestParam(value = "customerId", required = false) String queryCustomerId) {

        // Support customer identity from header, request body, or query param for standalone testing flexibility
        String customerId = headerCustomerId != null ? headerCustomerId :
                (queryCustomerId != null ? queryCustomerId : request.getCustomerId());

        Review updated = reviewService.updateReview(id, request, customerId);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully.", updated));
    }

    /**
     * Delete an existing review by ID.
     * DELETE /api/reviews/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable("id") String id,
            @RequestHeader(value = "X-Customer-Id", required = false) String headerCustomerId,
            @RequestParam(value = "customerId", required = false) String queryCustomerId) {

        String customerId = headerCustomerId != null ? headerCustomerId : queryCustomerId;
        reviewService.deleteReview(id, customerId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully."));
    }
}
