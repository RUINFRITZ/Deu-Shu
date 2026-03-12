package com.deushu.store.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * store_images 테이블 매핑 DTO
 * 가게의 여러 사진을 관리하는 데이터 전송 객체
 */
@Data
public class StoreImageDto {

    private Long id;
    private Long storeId;

    // S3 버킷 또는 외부 이미지 URL
    private String imageUrl;

    // 표시 순서 (UI 렌더링용)
    private int sortOrder;

    private LocalDateTime createdAt;
}