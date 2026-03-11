package com.deushu.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객용 QR 조회 응답 DTO
 * GET /api/v1/orders/{orderId}/qr
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQrDto {

    private Long   orderId;
    private String pickupCode;    // QR 내용 (UUID)
    private String orderStatus;   // PAYMENT_COMPLETED / PICKUP_COMPLETED 등
    private int    totalPrice;
    private String storeName;
    private String createdAt;     // 포맷: yyyy-MM-dd HH:mm
}