package com.deushu.store.dto;

import lombok.Data;

@Data
public class StoreDetailRows {

    @Data
    public static class StoreBaseRow {
        private Long id;
        private String name;
        private String category;
        private String address;
        private Double lat;
        private Double lng;
        private String thumbnailUrl;
        private String openTime;
        private String closeTime;
    }

    @Data
    public static class RatingAggRow {
        private Double avgRating;
        private Integer reviewCount;
    }
}