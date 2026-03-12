package com.deushu.review.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewListResponse {
    private List<ReviewItem> items;
    private Long nextCursor;

    @Data
    public static class ReviewItem {
        private Long id;
        private Long memberId;
        private String firstName;       // 성씨만 표시 (예: 一郎)
        private String orderedItems;    // 주문 상품 목록 (예: 초밥 세트, 된장국)
        private LocalDateTime orderDate;
        private String title;
        private String content;
        private Double rating;
        private String photoUrl;
        private LocalDateTime createdAt;
    }
}