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
        private String memberName;   // members.name join해서 내려줄 값
        private String title;
        private String content;
        private Double rating;
        private String photoUrl;
        private LocalDateTime createdAt;
    }
}