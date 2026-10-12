package com.bookstore.review.repository;

import com.bookstore.review.model.Review;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonFileReviewRepository Persistence Tests")
class JsonFileReviewRepositoryTest {

    @TempDir
    Path tempDir;

    private JsonFileReviewRepository repository;
    private String testFilePath;

    @BeforeEach
    void setUp() {
        testFilePath = tempDir.resolve("test-reviews.json").toString();
        repository = new JsonFileReviewRepository(testFilePath);
        repository.init();
    }

    @Test
    @DisplayName("Should create file when initializing repository on nonexistent path")
    void testInitCreatesFile() {
        assertTrue(Files.exists(Path.of(testFilePath)));
        assertEquals(0, repository.count());
    }

    @Test
    @DisplayName("Should save a new review, assign unique ID, and persist to file")
    void testSaveAndRetrieve() {
        Review review = new Review(null, "BK-101", "CUST-001", 5, "Great read", LocalDateTime.now(), LocalDateTime.now());
        Review saved = repository.save(review);

        assertNotNull(saved.getReviewId());
        assertTrue(saved.getReviewId().startsWith("REV-"));
        assertEquals(1, repository.count());

        Optional<Review> found = repository.findById(saved.getReviewId());
        assertTrue(found.isPresent());
        assertEquals("BK-101", found.get().getBookId());
        assertEquals(5, found.get().getRating());

        // Verify another repository instance reading the same file sees the record (true persistence)
        JsonFileReviewRepository reloadedRepo = new JsonFileReviewRepository(testFilePath);
        reloadedRepo.init();
        assertEquals(1, reloadedRepo.count());
        assertTrue(reloadedRepo.findById(saved.getReviewId()).isPresent());
    }

    @Test
    @DisplayName("Should find reviews by bookId and customerId")
    void testFindByBookIdAndCustomerId() {
        repository.save(new Review(null, "BK-101", "CUST-001", 5, "Review 1", LocalDateTime.now(), LocalDateTime.now()));
        repository.save(new Review(null, "BK-101", "CUST-002", 4, "Review 2", LocalDateTime.now(), LocalDateTime.now()));
        repository.save(new Review(null, "BK-102", "CUST-001", 3, "Review 3", LocalDateTime.now(), LocalDateTime.now()));

        List<Review> bk101Reviews = repository.findByBookId("BK-101");
        assertEquals(2, bk101Reviews.size());

        List<Review> cust001Reviews = repository.findByCustomerId("CUST-001");
        assertEquals(2, cust001Reviews.size());

        assertTrue(repository.existsByBookIdAndCustomerId("BK-101", "CUST-001"));
        assertFalse(repository.existsByBookIdAndCustomerId("BK-102", "CUST-002"));
    }

    @Test
    @DisplayName("Should delete a review by ID and update persistent storage")
    void testDeleteById() {
        Review saved = repository.save(new Review(null, "BK-101", "CUST-001", 5, "Test delete", LocalDateTime.now(), LocalDateTime.now()));
        String id = saved.getReviewId();

        assertTrue(repository.existsById(id));
        boolean deleted = repository.deleteById(id);
        assertTrue(deleted);
        assertFalse(repository.existsById(id));
        assertEquals(0, repository.count());

        // Delete nonexistent ID should return false safely
        assertFalse(repository.deleteById("NONEXISTENT-ID"));
    }

    @Test
    @DisplayName("Should update existing review in place without creating duplicates")
    void testUpdateExistingReview() {
        Review saved = repository.save(new Review(null, "BK-101", "CUST-001", 4, "Initial comment", LocalDateTime.now(), LocalDateTime.now()));
        String id = saved.getReviewId();

        saved.updateContent(5, "Updated comment text");
        repository.save(saved);

        assertEquals(1, repository.count());
        Review retrieved = repository.findById(id).orElseThrow();
        assertEquals(5, retrieved.getRating());
        assertEquals("Updated comment text", retrieved.getComment());
    }
}
