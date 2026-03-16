package com.deushu.store.controller;

import com.deushu.store.dto.ReviewSummaryResponseDto;
import com.deushu.store.service.GeminiReviewSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 리뷰 요약 API 컨트롤러
 * SecurityConfig 에서 GET /api/stores/** 는 전체 공개 처리되어 있으므로
 * 별도 인증 설정 불필요
 *
 * 엔드포인트: GET /api/stores/{storeId}/review-summary
 */
@Slf4j
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class ReviewSummaryController {

    private final GeminiReviewSummaryService geminiReviewSummaryService;

    /**
     * 특정 가게의 AI 리뷰 요약 조회
     * detail.html 로딩 시 JS에서 비동기 호출
     *
     * @param storeId 가게 ID (PathVariable)
     * @return ReviewSummaryResponseDto (리뷰 없으면 hasReviews=false)
     */
    @GetMapping("/{storeId}/review-summary")
    public ResponseEntity<ReviewSummaryResponseDto> getReviewSummary(
            @PathVariable("storeId") Long storeId
    ) {
        log.info("[AI 요약] 요청 수신: storeId={}", storeId);

        ReviewSummaryResponseDto result = geminiReviewSummaryService.summarize(storeId);

        log.info("[AI 요약] 응답 완료: storeId={}, hasReviews={}", storeId, result.isHasReviews());
        return ResponseEntity.ok(result);
    }
}