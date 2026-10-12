package com.bookstore.review.repository;

import com.bookstore.review.exception.StorageException;
import com.bookstore.review.model.Review;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * Robust JSON File-based implementation of ReviewRepository.
 * Demonstrates:
 * - Polymorphism: Concrete implementation of ReviewRepository interface.
 * - Concurrency Control: Uses ReentrantReadWriteLock for thread-safe access.
 * - Atomic Writes: Writes to a temporary file first before atomically replacing the target file,
 *   preventing file corruption in case of unexpected process termination.
 * - Defensive File Handling: Auto-creates directories and handles missing/corrupted files gracefully.
 */
@Repository
public class JsonFileReviewRepository implements ReviewRepository {

    private static final Logger logger = LoggerFactory.getLogger(JsonFileReviewRepository.class);

    private final String filePathString;
    private final ObjectMapper objectMapper;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    // In-memory map acting as primary indexed storage, backed by the JSON file
    private final Map<String, Review> reviewsCache = new LinkedHashMap<>();

    public JsonFileReviewRepository(@Value("${app.storage.file-path:data/reviews.json}") String filePath) {
        this.filePathString = filePath;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @PostConstruct
    public void init() {
        rwLock.writeLock().lock();
        try {
            loadFromFile();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Loads reviews from the persistent JSON file into the cache.
     * Safely creates the file and directory if they do not exist.
     */
    private void loadFromFile() {
        Path path = Paths.get(filePathString);
        try {
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
                logger.info("Created storage directory: {}", path.getParent().toAbsolutePath());
            }

            File file = path.toFile();
            if (!file.exists()) {
                logger.warn("Storage file does not exist at {}. Creating empty reviews file.", path.toAbsolutePath());
                Files.write(path, "[]".getBytes());
                reviewsCache.clear();
                return;
            }

            if (file.length() == 0) {
                logger.warn("Storage file is empty. Initializing with empty list.");
                Files.write(path, "[]".getBytes());
                reviewsCache.clear();
                return;
            }

            List<Review> loadedList = objectMapper.readValue(file, new TypeReference<List<Review>>() {});
            reviewsCache.clear();
            if (loadedList != null) {
                for (Review r : loadedList) {
                    if (r != null && r.getReviewId() != null) {
                        reviewsCache.put(r.getReviewId(), r);
                    }
                }
            }
            logger.info("Successfully loaded {} reviews from file storage: {}", reviewsCache.size(), path.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to load review records from file: {}", path.toAbsolutePath(), e);
            throw new StorageException("Unable to initialize review storage file: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically writes current cached reviews to the persistent file.
     * Must be called within an active write lock.
     */
    private void saveToFile() {
        Path targetPath = Paths.get(filePathString);
        Path parentDir = targetPath.getParent();
        try {
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // Write to a temporary file first
            Path tempPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".tmp");
            List<Review> reviewList = new ArrayList<>(reviewsCache.values());
            objectMapper.writeValue(tempPath.toFile(), reviewList);

            // Atomically replace target file
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                // Fallback for file systems where atomic move is not supported across volumes
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            logger.debug("Persisted {} reviews to file: {}", reviewList.size(), targetPath.toAbsolutePath());
        } catch (IOException e) {
            logger.error("Failed to persist reviews to file: {}", targetPath.toAbsolutePath(), e);
            throw new StorageException("Error writing reviews to persistent file storage: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a unique, collision-free review ID in the format REV-1001, REV-1002, etc.
     */
    private synchronized String generateNextId() {
        int maxId = 1000;
        for (String id : reviewsCache.keySet()) {
            if (id != null && id.startsWith("REV-")) {
                try {
                    int num = Integer.parseInt(id.substring(4));
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {
                    // Ignore IDs with custom non-numeric suffix
                }
            }
        }
        return "REV-" + (maxId + 1);
    }

    @Override
    public List<Review> findAll() {
        rwLock.readLock().lock();
        try {
            return new ArrayList<>(reviewsCache.values());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Optional<Review> findById(String reviewId) {
        if (reviewId == null) {
            return Optional.empty();
        }
        rwLock.readLock().lock();
        try {
            return Optional.ofNullable(reviewsCache.get(reviewId.trim()));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Review> findByBookId(String bookId) {
        if (bookId == null) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            return reviewsCache.values().stream()
                    .filter(r -> bookId.trim().equalsIgnoreCase(r.getBookId()))
                    .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public List<Review> findByCustomerId(String customerId) {
        if (customerId == null) {
            return Collections.emptyList();
        }
        rwLock.readLock().lock();
        try {
            return reviewsCache.values().stream()
                    .filter(r -> customerId.trim().equalsIgnoreCase(r.getCustomerId()))
                    .collect(Collectors.toList());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean existsById(String reviewId) {
        if (reviewId == null) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            return reviewsCache.containsKey(reviewId.trim());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean existsByBookIdAndCustomerId(String bookId, String customerId) {
        if (bookId == null || customerId == null) {
            return false;
        }
        rwLock.readLock().lock();
        try {
            return reviewsCache.values().stream()
                    .anyMatch(r -> bookId.trim().equalsIgnoreCase(r.getBookId()) &&
                            customerId.trim().equalsIgnoreCase(r.getCustomerId()));
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Review save(Review review) {
        if (review == null) {
            throw new IllegalArgumentException("Cannot save null review.");
        }

        rwLock.writeLock().lock();
        try {
            if (review.getReviewId() == null || review.getReviewId().trim().isEmpty()) {
                review.setReviewId(generateNextId());
            }

            if (review.getCreatedAt() == null) {
                review.setCreatedAt(LocalDateTime.now());
            }
            if (review.getUpdatedAt() == null) {
                review.setUpdatedAt(review.getCreatedAt());
            }

            reviewsCache.put(review.getReviewId(), review);
            saveToFile();
            return review;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public boolean deleteById(String reviewId) {
        if (reviewId == null) {
            return false;
        }

        rwLock.writeLock().lock();
        try {
            Review removed = reviewsCache.remove(reviewId.trim());
            if (removed != null) {
                saveToFile();
                return true;
            }
            return false;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public long count() {
        rwLock.readLock().lock();
        try {
            return reviewsCache.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void deleteAll() {
        rwLock.writeLock().lock();
        try {
            reviewsCache.clear();
            saveToFile();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
