package com.deushu.order.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/v1/orders/my?storeId={storeId}
 * 내 결제 완료 주문 목록 응답 DTO
 */
@Data
public class MyOrderResponse {
    private Long orderId;
    private String storeName;
    private LocalDateTime createdAt;
    private Integer totalPrice;
    private String orderStatus; // 인재 추가
    private List<OrderItemSummary> items;
    private boolean reviewed;   // 이미 리뷰 작성했는지 여부

    @Data
    public static class OrderItemSummary {
        private Long itemId;
        private String itemName;
        private Integer quantity;
        private Integer orderPrice;
        private String thumbnailUrl;
    }
}