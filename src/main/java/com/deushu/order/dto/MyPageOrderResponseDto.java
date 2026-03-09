// =========================================================================
// [ ドゥーシュー ] マイページ注文一覧レスポンス用 DTO
// =========================================================================

package com.deushu.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyPageOrderResponseDto {
    private Long orderId;          // 注文ID
    private String storeName;      // 店舗名
    private Integer totalPrice;    // 決済金額
    private String pickupCode;     // QR生成用のUUID
    private String orderStatus;    // 注文ステータス (PAYMENT_COMPLETED)
    private String orderTime;      // 決済時間 (HH:mm 形式)
    private Integer itemCount;     // 注文した商品種類の数 (例: ~外 N件用)
}