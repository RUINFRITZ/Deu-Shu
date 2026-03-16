package com.deushu.store.dto;

import lombok.Data;

/**
 * AI 리뷰 요약 처리용 DTO
 * DB에서 가져온 리뷰 원본 데이터를 담는다.
 * Gemini API로 전송할 최소 필드만 포함 (토큰 절약)
 */
@Data
public class ReviewForSummaryDto {
    private String title;    // 리뷰 제목
    private String content;  // 리뷰 본문
    private Double rating;   // 별점 (1.0 ~ 5.0)
}