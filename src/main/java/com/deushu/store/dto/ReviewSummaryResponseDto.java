package com.deushu.store.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * AI 리뷰 요약 최종 응답 DTO
 * Gemini Map-Reduce 결과를 파싱해서 프론트엔드로 반환
 */
@Data
@Builder
public class ReviewSummaryResponseDto {
    private boolean hasReviews;
    private String summary;          // 통합 요약 문장
    private List<String> keywords;   // 주요 키워드 리스트
}