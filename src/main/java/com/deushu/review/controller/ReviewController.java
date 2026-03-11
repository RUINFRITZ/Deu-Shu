package com.deushu.review.controller;

import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse;
import com.deushu.review.service.ReviewService;
import com.deushu.store.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final S3Service     s3Service;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.region}")
    private String region;

    // GET /api/reviews?storeId=1
    @GetMapping
    public ReviewListResponse list(@RequestParam("storeId") long storeId) {
        return reviewService.listByStore(storeId);
    }

    // POST /api/reviews
    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ReviewCreateRequest req) {
        Long memberId = getSessionMemberId();
        if (memberId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));
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

    // POST /api/reviews/image — 리뷰 이미지 S3 업로드
    @PostMapping("/image")
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty())
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "파일이 없습니다."));

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/"))
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "이미지 파일만 업로드 가능합니다."));

        if (file.getSize() > 5 * 1024 * 1024)
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "파일 크기는 5MB 이하여야 합니다."));

        try {
            String ext = "";
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains("."))
                ext = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

            String key = "reviews/" + java.util.UUID.randomUUID() + ext;
            s3Service.upload(file, key);

            String url = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            log.info("[리뷰 이미지 업로드 완료] url={}", url);
            return ResponseEntity.ok(Map.of("success", true, "url", url));

        } catch (Exception e) {
            log.error("[리뷰 이미지 업로드 실패]", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "업로드 중 오류가 발생했습니다."));
        }
    }

    // DELETE /api/reviews/{reviewId}
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable("reviewId") long reviewId) {
        Long memberId = getSessionMemberId();
        if (memberId == null)
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "로그인이 필요합니다."));

        try {
            reviewService.delete(reviewId, memberId);
            return ResponseEntity.ok(Map.of("success", true));
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