package com.bookstore.review.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Data Transfer Object (DTO) for incoming create and update review requests.
 * Uses standard Jakarta Bean Validation annotations to enforce input boundaries.
 */
public class ReviewRequest {

    @NotBlank(message = "Book ID is required and cannot be blank.")
    private String bookId;

    @NotBlank(message = "Customer ID is required and cannot be blank.")
    private String customerId;

    @NotNull(message = "Rating is required.")
    @Min(value = 1, message = "Rating must be a whole number between 1 and 5.")
    @Max(value = 5, message = "Rating must be a whole number between 1 and 5.")
    private Integer rating;

    @NotBlank(message = "Comment is required and cannot be empty or only whitespace.")
    @Size(max = 1000, message = "Comment cannot exceed 1000 characters.")
    private String comment;

    public ReviewRequest() {
    }

    public ReviewRequest(String bookId, String customerId, Integer rating, String comment) {
        this.bookId = bookId;
        this.customerId = customerId;
        this.rating = rating;
        this.comment = comment;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
