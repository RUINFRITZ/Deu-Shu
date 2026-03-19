package com.deushu.order.dto;

import lombok.Data;

/**
 * 오너 페이지 — 결제 완료(PAYMENT_COMPLETED) 주문 목록 표시용 DTO
 *
 * GET /api/owner/orders/pending-pickup 응답으로 사용.
 * ownerPage.html section-pickup 하단 "入金済み注文リスト" 에 렌더링.
 */
@Data
public class PendingPickupOrderDto {

    /** orders.id */
    private Long   orderId;

    /** orders.pickup_code — QR 스캐너 수동 입력 시 참고 */
    private String pickupCode;

    /** orders.total_price */
    private Integer totalPrice;

    /** order_items 건수 (상품 종류 수) */
    private Integer itemCount;

    /** orders.created_at (yyyy-MM-dd HH:mm 포맷) */
    private String createdAt;
    
    // "鮭弁当 ×2 / 唐揚げ弁当 ×1"
    private String itemSummary;
}