package com.deushu.store.domain;

import lombok.Data;

import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
public class StoreEntity {
    private Long id;
    private Long ownerId;

    private String name;
    private String businessNumber;
    private String category;

    private String address;
    private Double lat;
    private Double lng;

    private LocalTime openTime;
    private LocalTime closeTime;

    private String thumbnailUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}