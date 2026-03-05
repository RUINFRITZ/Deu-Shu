package com.deushu.store.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * FR-M04
 * 지도 필터 검색 요청 파라미터 DTO
 * GET /api/stores/filter 의 @ModelAttribute 혹은 @RequestParam 집합
 */
@Getter
@Setter
public class StoreFilterRequest {

    /* ── 위치 기반 파라미터 ── */

    /** 지도 중심 위도 (선택) */
    private Double centerLat;

    /** 지도 중심 경도 (선택) */
    private Double centerLng;

    /** 조회 반경 km (선택, null이면 전체) */
    private Double radius;

    /* ── 카테고리 필터 ── */

    /** stores.category (BAKERY / SUSHI / LUNCHBOX / CAFE / SIDEDISH, 선택) */
    private String category;

    /* ── 할인율 필터 ── */

    /** items.discount_rate >= minDiscountRate */
    private Integer minDiscountRate;

    /** items.discount_rate <= maxDiscountRate */
    private Integer maxDiscountRate;

    /* ── 가격 필터 ── */

    /** items.discount_price <= maxDiscountPrice */
    private Integer maxDiscountPrice;

    /* ── 마감 시간 필터 ── */

    /**
     * 현재 시각 + expireWithinMinutes 이내 마감 상품만 조회
     * 예: 120 → items.expire_at <= NOW() + INTERVAL 120 MINUTE
     */
    private Integer expireWithinMinutes;

    /* ── 정렬 ── */

    /**
     * 정렬 기준 (선택, 기본: "stock")
     * stock       → 재고 적은 순 (마감 임박 우선)
     * discountRate → 할인율 높은 순
     * distance    → 거리 가까운 순 (centerLat/centerLng 필수)
     */
    private String sortBy = "stock";
}