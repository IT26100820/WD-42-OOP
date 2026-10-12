package com.bookstore.review.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Review Domain Model Unit Tests")
class ReviewTest {

    @Test
    @DisplayName("Should create valid Review instance with all properties set")
    void testValidReviewCreation() {
        LocalDateTime now = LocalDateTime.now();
        Review review = new Review("REV-1001", "BK-101", "CUST-001", 5, "Outstanding book!", now, now);

        assertEquals("REV-1001", review.getReviewId());
        assertEquals("BK-101", review.getBookId());
        assertEquals("CUST-001", review.getCustomerId());
        assertEquals(5, review.getRating());
        assertEquals("Outstanding book!", review.getComment());
        assertEquals(now, review.getCreatedAt());
        assertEquals(now, review.getUpdatedAt());
    }

    @Test
    @DisplayName("Should update content and update timestamp while preserving reviewId")
    void testUpdateContent() throws InterruptedException {
        LocalDateTime created = LocalDateTime.now().minusHours(1);
        Review review = new Review("REV-1001", "BK-101", "CUST-001", 4, "Initial review", created, created);

        Thread.sleep(10); // Guarantee timestamp progression
        review.updateContent(5, "Updated comment after finishing reading");

        assertEquals(5, review.getRating());
        assertEquals("Updated comment after finishing reading", review.getComment());
        assertEquals(created, review.getCreatedAt());
        assertTrue(review.getUpdatedAt().isAfter(created));
    }

    @Test
    @DisplayName("Should reject invalid ratings lower than 1 or greater than 5")
    void testRejectInvalidRatings() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", 0, "Test", now, now));

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", 6, "Test", now, now));

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", -1, "Test", now, now));

        Review review = new Review("REV-1", "BK-1", "CUST-1", 3, "Test", now, now);
        assertThrows(IllegalArgumentException.class, () -> review.setRating(7));
    }

    @Test
    @DisplayName("Should reject empty or whitespace comments")
    void testRejectEmptyComment() {
        LocalDateTime now = LocalDateTime.now();

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", 4, "", now, now));

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", 4, "   \t\n  ", now, now));

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", 4, null, now, now));
    }

    @Test
    @DisplayName("Should reject comments exceeding 1000 characters")
    void testRejectTooLongComment() {
        LocalDateTime now = LocalDateTime.now();
        String overLongComment = "A".repeat(1001);

        assertThrows(IllegalArgumentException.class, () ->
                new Review("REV-1", "BK-1", "CUST-1", 4, overLongComment, now, now));
    }

    @Test
    @DisplayName("Should compare reviews with newest first by createdAt")
    void testCompareTo() {
        LocalDateTime t1 = LocalDateTime.now().minusDays(2);
        LocalDateTime t2 = LocalDateTime.now().minusDays(1);

        Review older = new Review("REV-1", "BK-1", "CUST-1", 4, "Older", t1, t1);
        Review newer = new Review("REV-2", "BK-1", "CUST-2", 5, "Newer", t2, t2);

        // newer.compareTo(older) should be negative (< 0) because newer comes before older in descending sort
        assertTrue(newer.compareTo(older) < 0);
    }
}
