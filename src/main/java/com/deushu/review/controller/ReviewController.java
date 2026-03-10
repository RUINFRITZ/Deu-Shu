package com.deushu.review.controller;

import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse;
import com.deushu.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    // GET /api/reviews?storeId=1
    @GetMapping
    public ReviewListResponse list(@RequestParam("storeId") long storeId) {
        return reviewService.listByStore(storeId);
    }

    // POST /api/reviews
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ReviewCreateRequest req) {
        // 세션(Security principal)에서 memberId 주입 — 클라이언트에서 받지 않음
        Long memberId = getSessionMemberId();
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
        }
        req.setMemberId(memberId);

        try {
            reviewService.create(req);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    private Long getSessionMemberId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        return null;
    }
}