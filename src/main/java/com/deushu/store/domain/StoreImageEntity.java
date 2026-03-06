package com.deushu.store.domain;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoreImageEntity {
    private Long id;
    private Long storeId;

    private String imageUrl;
    private Integer sortOrder;

    private LocalDateTime createdAt;
}
