package com.deushu.store.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class StoreDetailResponse {

	private StoreInfo store;       // stores
    private List<String> images;   // store_images
    private RatingInfo rating;     // reviews 집계
    private List<ItemInfo> items;  // items 판매중

    @Data
    public static class StoreInfo {
        private Long id;
        private String name;
        private String category;
        private String address;
        private Double lat;
        private Double lng;
        private String thumbnailUrl;
        private String openTime;   // "10:00:00" 같이 TIME 문자열로 내려도 OK
        private String closeTime;
    }

    @Data
    public static class RatingInfo {
        private Double avg;   // 리뷰 0개면 0.0 처리
        private Integer count;
    }

    @Data
    public static class ItemInfo {
        private Long id;
        private String name;
        private Integer originalPrice;
        private Integer discountPrice;
        private Integer discountRate;
        private Integer stock;
        private LocalDateTime expireAt;
        private String thumbnailUrl;
    }
}