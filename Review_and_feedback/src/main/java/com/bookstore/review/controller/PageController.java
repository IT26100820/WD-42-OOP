package com.bookstore.review.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to handle web page view mappings for the static frontend interfaces.
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    @GetMapping("/review-test")
    public String reviewTest() {
        return "forward:/review-test.html";
    }
}
