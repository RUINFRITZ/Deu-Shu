package com.deushu.store.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 마이페이지 즐겨찾기 탭 — 가게 목록 응답 DTO
 * GET /api/favorites/my
 */
@Data
public class FavoriteStoreDto {
    private Long          storeId;
    private String        storeName;
    private String        category;
    private String        address;
    private String        openTime;
    private String        closeTime;
    private String        thumbnailUrl;
    private LocalDateTime favoritedAt;
}