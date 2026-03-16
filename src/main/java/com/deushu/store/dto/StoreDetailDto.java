package com.deushu.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * FR-M03
 * 지도 팝업 / 우측 사이드 패널용 가게 상세 DTO
 * stores + items + store_images + favorites 를 합산한 복합 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDetailDto {

    /* ── 가게 기본 정보 ── */
    private Long   id;
    private String name;
    private String category;
    private String address;
    private Double lat;
    private Double lng;
    private String openTime;         // stores.open_time
    private String closeTime;        // stores.close_time
    private String thumbnailUrl;     // stores.thumbnail_url
    private String info;          
    
    /* ── 활성 마감 할인 상품 목록 ── */
    private List<ItemSummaryDto> items;

    /* ── 가게 이미지 목록 ── */
    private List<String> imageUrls;  // store_images.image_url (sort_order 순)

    /* ── 즐겨찾기 여부 (로그인 시에만 의미 있음) ── */
    private boolean favorited;

    /* ── 집계 통계 ── */
    private Double avgRating;        // AVG(reviews.rating)
    private Integer reviewCount;     // COUNT(reviews.id)

    /**
     * FR-M03
     * 상품 목록에서 사용하는 내부 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemSummaryDto {
        private Long   id;
        private String name;
        private Integer originalPrice;
        private Integer discountPrice;
        private Integer discountRate;
        private Integer stock;
        private String  expireAt;     // items.expire_at (yyyy-MM-dd HH:mm 포맷)
        private String  thumbnailUrl; // items.thumbnail_url
    }
}