package com.bookstore.review.controller;

import com.bookstore.review.dto.ReviewRequest;
import com.bookstore.review.exception.DuplicateReviewException;
import com.bookstore.review.exception.GlobalExceptionHandler;
import com.bookstore.review.exception.ResourceNotFoundException;
import com.bookstore.review.exception.UnauthorizedReviewException;
import com.bookstore.review.model.Review;
import com.bookstore.review.service.ReviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("ReviewController REST Endpoint Tests")
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @Test
    @DisplayName("GET /api/reviews/health should return 200 OK with UP status")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/reviews/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    @DisplayName("GET /api/reviews should return 200 OK with list of reviews")
    void testGetAllReviews() throws Exception {
        Review review = new Review("REV-1001", "BK-101", "CUST-001", 5, "Great book", LocalDateTime.now(), LocalDateTime.now());
        when(reviewService.getAllReviews(any(), any(), any(), any())).thenReturn(Collections.singletonList(review));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].reviewId").value("REV-1001"));
    }

    @Test
    @DisplayName("GET /api/reviews/{id} should return 200 OK when review exists")
    void testGetReviewByIdSuccess() throws Exception {
        Review review = new Review("REV-1001", "BK-101", "CUST-001", 5, "Great book", LocalDateTime.now(), LocalDateTime.now());
        when(reviewService.getReviewById("REV-1001")).thenReturn(review);

        mockMvc.perform(get("/api/reviews/REV-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value("REV-1001"));
    }

    @Test
    @DisplayName("GET /api/reviews/{id} should return 404 NOT FOUND when review does not exist")
    void testGetReviewByIdNotFound() throws Exception {
        when(reviewService.getReviewById("REV-999999"))
                .thenThrow(new ResourceNotFoundException("Review not found with ID: REV-999999"));

        mockMvc.perform(get("/api/reviews/REV-999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Review not found with ID: REV-999999"));
    }

    @Test
    @DisplayName("POST /api/reviews should return 201 CREATED for valid review submission")
    void testCreateReviewSuccess() throws Exception {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-001", 5, "Terrific read!");
        Review created = new Review("REV-1001", "BK-101", "CUST-001", 5, "Terrific read!", LocalDateTime.now(), LocalDateTime.now());

        when(reviewService.createReview(any(ReviewRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewId").value("REV-1001"));
    }

    @Test
    @DisplayName("POST /api/reviews should return 400 BAD REQUEST when rating is out of bounds (6)")
    void testCreateReviewInvalidRating() throws Exception {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-001", 6, "Invalid rating");

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors.rating").exists());
    }

    @Test
    @DisplayName("POST /api/reviews should return 400 BAD REQUEST when comment is empty or blank")
    void testCreateReviewEmptyComment() throws Exception {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-001", 5, "   ");

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors.comment").exists());
    }

    @Test
    @DisplayName("POST /api/reviews should return 409 CONFLICT for duplicate review submission")
    void testCreateReviewDuplicateConflict() throws Exception {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-001", 5, "Duplicate review");

        when(reviewService.createReview(any(ReviewRequest.class)))
                .thenThrow(new DuplicateReviewException("Customer 'CUST-001' has already submitted a review for book 'BK-101'."));

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("already submitted a review")));
    }

    @Test
    @DisplayName("PUT /api/reviews/{id} should return 403 FORBIDDEN when user is unauthorized")
    void testUpdateReviewUnauthorized() throws Exception {
        ReviewRequest request = new ReviewRequest("BK-101", "CUST-002", 4, "Unauthorized edit");

        when(reviewService.updateReview(eq("REV-1001"), any(ReviewRequest.class), eq("CUST-002")))
                .thenThrow(new UnauthorizedReviewException("You do not have permission to edit this review."));

        mockMvc.perform(put("/api/reviews/REV-1001")
                        .header("X-Customer-Id", "CUST-002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @DisplayName("DELETE /api/reviews/{id} should return 200 OK on successful deletion")
    void testDeleteReviewSuccess() throws Exception {
        mockMvc.perform(delete("/api/reviews/REV-1001")
                        .header("X-Customer-Id", "CUST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
