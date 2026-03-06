package com.deushu.review.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewEntity {
    private Long id;
    private Long memberId;
    private Long storeId;
    private Long orderId;

    private String title;
    private String content;

    private String photoUrl;
    private Integer rating;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}