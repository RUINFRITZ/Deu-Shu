package com.deushu.store.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * FR-M01 / FR-M02
 * 지도 핀 표시용 DTO
 * stores + items 집계 결과를 담는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMapDto {

    /** stores.id */
    private Long id;

    /** stores.name */
    private String name;

    /** stores.lat */
    private Double lat;

    /** stores.lng */
    private Double lng;

    /** stores.category */
    private String category;

    /** stores.close_time (HH:mm 포맷) */
    private String closeTime;

    /** stores.thumbnail_url */
    private String thumbnailUrl;

    /** SUM(items.stock) — 해당 가게의 전체 잔여 수량 */
    private Integer totalStock;

    /** MIN(items.discount_price) — 최저 할인가 */
    private Integer minDiscountPrice;

    /** MIN(items.original_price) — 최저 원가 */
    private Integer minOriginalPrice;

    /** MAX(items.discount_rate) — 최대 할인율 */
    private Integer maxDiscountRate;

    /**
     * 중심 좌표 기준 직선거리 (km)
     * Haversine 공식으로 DB에서 계산 후 주입
     * center_lat / center_lng 가 없으면 null
     */
    private Double distanceKm;
}