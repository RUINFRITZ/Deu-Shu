package com.deushu.review.dto;

import lombok.Data;

@Data
public class ReviewCreateRequest {
    private Long storeId;
    private Long orderId;     // ✅ 필수
    private Long memberId;    // ✅ 임시(나중엔 로그인에서 꺼내기)
    private String title;
    private String content;
    private Double rating;
    private String photoUrl;
}