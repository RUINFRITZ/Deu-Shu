package com.deushu.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오너 픽업 확인 응답 DTO
 * GET /api/owner/pickup/{pickupCode}
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PickupVerifyResponse {

    private Long   orderId;
    private String orderStatus;   // PAYMENT_COMPLETED, PICKUP_COMPLETED, CANCELED 등
    private int    totalPrice;
    private int    itemCount;     // 주문 상품 종류 수
    private String createdAt;     // yyyy-MM-dd HH:mm
    private Long   storeId;
}