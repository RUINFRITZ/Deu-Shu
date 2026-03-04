package com.deushu.review.controller;

import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse;
import com.deushu.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // /api/reviews?storeId=1
    @GetMapping
    public ReviewListResponse list(@RequestParam("storeId") long storeId) {
        return reviewService.listByStore(storeId);
    }
    
    @PostMapping
    public void create(@RequestBody ReviewCreateRequest req) {
        reviewService.create(req);
    }
    
}