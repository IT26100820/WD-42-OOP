package com.bookstore.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Main entry point for the Review and Feedback Management Module.
 * Online Bookstore Management System.
 */
@SpringBootApplication
public class ReviewApplication {

    private static final Logger logger = LoggerFactory.getLogger(ReviewApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ReviewApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        System.out.println("==================================================================");
        System.out.println("  ONLINE BOOKSTORE - REVIEW & FEEDBACK MANAGEMENT MODULE STARTED  ");
        System.out.println("==================================================================");
        System.out.println("  * Customer Review UI:         http://localhost:8080/");
        System.out.println("  * Dedicated Backend Tester:   http://localhost:8080/review-test");
        System.out.println("  * REST API Base:              http://localhost:8080/api/reviews");
        System.out.println("  * Health Check Endpoint:      http://localhost:8080/api/reviews/health");
        System.out.println("  * Persistent Storage File:    data/reviews.json");
        System.out.println("==================================================================");
        logger.info("Application ready to accept requests.");
    }
}
