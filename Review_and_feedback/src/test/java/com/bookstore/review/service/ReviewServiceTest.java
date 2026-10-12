package com.bookstore.review.service;

import com.bookstore.review.dto.BookReviewSummary;
import com.bookstore.review.dto.ReviewRequest;
import com.bookstore.review.exception.DuplicateReviewException;
import com.bookstore.review.exception.ResourceNotFoundException;
import com.bookstore.review.exception.UnauthorizedReviewException;
import com.bookstore.review.model.Review;
import com.bookstore.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewService Business Logic Tests")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewRepository, true);
    }

    @Test
    @DisplayName("Should successfully create a review when input is valid and not duplicate")
    void testCreateReviewSuccess() {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-001", 5, "Fabulous book!");

        when(reviewRepository.existsByBookIdAndCustomerId("BK-101", "CUST-001")).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setReviewId("REV-1001");
            return r;
        });

        Review created = reviewService.createReview(request);

        assertNotNull(created);
        assertEquals("REV-1001", created.getReviewId());
        assertEquals("BK-101", created.getBookId());
        assertEquals("CUST-001", created.getCustomerId());
        assertEquals(5, created.getRating());
        assertEquals("Fabulous book!", created.getComment());
        verify(reviewRepository, times(1)).save(any(Review.class));
    }

    @Test
    @DisplayName("Should throw DuplicateReviewException when customer has already reviewed the book")
    void testCreateDuplicateReviewThrows() {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-001", 5, "Second review attempt");

        when(reviewRepository.existsByBookIdAndCustomerId("BK-101", "CUST-001")).thenReturn(true);

        assertThrows(DuplicateReviewException.class, () -> reviewService.createReview(request));
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when review ID does not exist")
    void testGetReviewByIdNotFound() {
        when(reviewRepository.findById("REV-999999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.getReviewById("REV-999999"));
    }

    @Test
    @DisplayName("Should successfully update review when requester is the owner")
    void testUpdateReviewSuccess() {
        LocalDateTime created = LocalDateTime.now().minusDays(1);
        Review existing = new Review("REV-1001", "BK-101", "CUST-001", 4, "Initial review", created, created);

        when(reviewRepository.findById("REV-1001")).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        ReviewRequest updateReq = new ReviewRequest("BK-101", "CUST-001", 5, "Updated to 5 stars");
        Review updated = reviewService.updateReview("REV-1001", updateReq, "CUST-001");

        assertEquals(5, updated.getRating());
        assertEquals("Updated to 5 stars", updated.getComment());
        assertEquals(created, updated.getCreatedAt()); // CreatedAt preserved
    }

    @Test
    @DisplayName("Should throw UnauthorizedReviewException when another customer attempts to edit")
    void testUpdateReviewUnauthorized() {
        Review existing = new Review("REV-1001", "BK-101", "CUST-001", 4, "Owner Alice", LocalDateTime.now(), LocalDateTime.now());
        when(reviewRepository.findById("REV-1001")).thenReturn(Optional.of(existing));

        ReviewRequest updateReq = new ReviewRequest("BK-101", "CUST-002", 1, "Malicious edit attempt");

        assertThrows(UnauthorizedReviewException.class, () ->
                reviewService.updateReview("REV-1001", updateReq, "CUST-002"));

        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("Should allow admin to edit or delete any review")
    void testAdminCanModifyAnyReview() {
        Review existing = new Review("REV-1001", "BK-101", "CUST-001", 4, "Alice review", LocalDateTime.now(), LocalDateTime.now());
        when(reviewRepository.findById("REV-1001")).thenReturn(Optional.of(existing));
        when(reviewRepository.deleteById("REV-1001")).thenReturn(true);

        assertDoesNotThrow(() -> reviewService.deleteReview("REV-1001", "admin"));
        verify(reviewRepository, times(1)).deleteById("REV-1001");
    }

    @Test
    @DisplayName("Should calculate correct book summary and rating distribution")
    void testBookSummaryCalculation() {
        List<Review> reviews = Arrays.asList(
                new Review("R1", "BK-101", "C1", 5, "Great", LocalDateTime.now(), LocalDateTime.now()),
                new Review("R2", "BK-101", "C2", 5, "Super", LocalDateTime.now(), LocalDateTime.now()),
                new Review("R3", "BK-101", "C3", 4, "Good", LocalDateTime.now(), LocalDateTime.now()),
                new Review("R4", "BK-101", "C4", 2, "Poor", LocalDateTime.now(), LocalDateTime.now())
        );

        when(reviewRepository.findByBookId("BK-101")).thenReturn(reviews);

        BookReviewSummary summary = reviewService.getBookSummary("BK-101");

        assertEquals("BK-101", summary.getBookId());
        assertEquals(4, summary.getTotalReviews());
        // Sum = 5 + 5 + 4 + 2 = 16. Avg = 16 / 4 = 4.0
        assertEquals(4.0, summary.getAverageRating());
        assertEquals(2L, summary.getRatingDistribution().get(5));
        assertEquals(1L, summary.getRatingDistribution().get(4));
        assertEquals(0L, summary.getRatingDistribution().get(3));
        assertEquals(1L, summary.getRatingDistribution().get(2));
        assertEquals(0L, summary.getRatingDistribution().get(1));
    }

    @Test
    @DisplayName("Should filter reviews by keyword search in comment")
    void testSearchInComments() {
        List<Review> reviews = Arrays.asList(
                new Review("R1", "BK-101", "C1", 5, "Loved the architecture patterns", LocalDateTime.now(), LocalDateTime.now()),
                new Review("R2", "BK-101", "C2", 4, "Great introduction to Java", LocalDateTime.now(), LocalDateTime.now())
        );

        when(reviewRepository.findAll()).thenReturn(reviews);

        List<Review> filtered = reviewService.getAllReviews(null, null, "architecture", "newest");
        assertEquals(1, filtered.size());
        assertEquals("R1", filtered.get(0).getReviewId());
    }
}
