package com.deushu.order.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
public class ItemEntity {
    private Long id;
    private Long storeId;
    private String name;
    private Integer originalPrice;
    private Integer discountPrice;
    private Integer discountRate;
    private Integer stock;
    private LocalDateTime expireAt;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    /**
     * DDD: 在庫を減少させるドメインロジック（Entity自身が状態を管理する）
     * (한국어: DDD: 재고를 감소시키는 도메인 로직 (Entity 자신이 상태를 관리함))
     */
    public void removeStock(int quantity) {
        int restStock = this.stock - quantity;
        if (restStock < 0) {
            throw new IllegalStateException("商品[" + this.name + "]の在庫が不足しています。残り: " + this.stock);
        }
        this.stock = restStock;
    }
}