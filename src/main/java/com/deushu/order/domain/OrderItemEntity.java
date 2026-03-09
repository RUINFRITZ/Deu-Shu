package com.deushu.order.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
public class OrderItemEntity {
    private Long id;
    private Long orderId;
    private Long itemId;
    private Integer quantity;
    private Integer orderPrice; // スナップショット価格
    private LocalDateTime createdAt;
}