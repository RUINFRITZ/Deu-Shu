package com.deushu.order.domain;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @ToString
@Builder @NoArgsConstructor @AllArgsConstructor
public class OrderEntity {
    private Long id;
    private Long memberId;
    private Long storeId;
    private String orderStatus; // ENUM: 'PAYMENT_PENDING', 'PAYMENT_COMPLETED', 'PICKUP_COMPLETED', 'CANCELED', 'EXPIRED'
    private Integer totalPrice;
    private String pickupCode; // UUID
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 준성 추가
    
    private String storeName;
    private String category ;
}