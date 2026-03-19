package com.deushu.order.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deushu.order.domain.ItemEntity;
import com.deushu.order.domain.OrderEntity;
import com.deushu.order.domain.OrderItemEntity;
import com.deushu.order.dto.MyOrderResponse;
import com.deushu.order.dto.PendingPickupOrderDto;

@Mapper
public interface OrderMapper {
    // 悲観的ロック(FOR UPDATE)をかけて商品情報を取得
    ItemEntity findItemByIdWithPessimisticLock(@Param("id") Long id);
    
    // 在庫数の更新
    void updateItemStock(ItemEntity item);

    // 注文(親)の生成 (生成されたPK(id)をOrderEntityにマッピングして返す)
    void insertOrder(OrderEntity order);

    // 注文詳細(子)のバルク(Bulk)生成
    void insertOrderItems(@Param("orderItems") List<OrderItemEntity> orderItems);
    
    /*
     * 注文ID(PK)で注文スナップショットを単一照会します。
     * PortOneの決済金額(amount)と、DBの total_price を比較(Cross-Validation)するために使用します。
     */
    OrderEntity findOrderById(@Param("id") Long id);

    /*
     * 注文のステータス（PAYMENT_PENDING -> PAYMENT_COMPLETED 等）を更新します。
     * 決済成功時、またはキャンセル時のみ呼び出されます。
     */
    void updateOrderStatus(OrderEntity order);
    
    // ════════════════════════════════════════════════════════════════
    // ★ 신규 추가 — QR / 픽업 확인용 (기존 메서드와 충돌 없음)
    // ════════════════════════════════════════════════════════════════

    /**
     * pickupCode(UUID)로 주문 단건 조회.
     * 오너 픽업 확인 시 사용.
     */
    OrderEntity findOrderByPickupCode(@Param("pickupCode") String pickupCode);

    /**
     * 주문 상품 종류 수 조회 (order_items 건수).
     * 오너 픽업 확인 화면에서 몇 가지 상품인지 표시.
     */
    int countOrderItems(@Param("orderId") Long orderId);

    /**
     * pickupCode 기반 상태 변경.
     * PAYMENT_COMPLETED → PICKUP_COMPLETED 처리.
     */
    void updateStatusByPickupCode(@Param("pickupCode") String pickupCode,
                                  @Param("status")     String status);
    /**
     * 해당 회원이 해당 가게에서 결제 완료(PAYMENT_COMPLETED)된 주문이 있는지 확인
     * ReviewService에서 리뷰 작성 자격 검증용
     */
    boolean existsCompletedOrder(@Param("memberId") Long memberId,
                                  @Param("storeId")  Long storeId);
    
    List<MyOrderResponse> findMyCompletedOrders(@Param("memberId") Long memberId,
            @Param("storeId")  Long storeId);
    
    // =====================================================================
    // スケジューラー用: 未決済状態(PAYMENT_PENDING)で指定時間経過した注文を照会
    // =====================================================================
    List<OrderEntity> findPendingOrdersOlderThan(@Param("minutes") int minutes);

    // ロールバック用: キャンセルされた注文に含まれる商品の在庫を復元(+quantity)
    void restoreItemStock(@Param("orderId") Long orderId);
    
    // キャンセルされた注文の商品履歴を再取得
    List<OrderItemEntity> findOrderItemsByOrderId(@Param("orderId") Long orderId);
    
    // ── ★ 오너 페이지: 결제 완료 대기 주문 목록 ─────────────────────────
    
    /**
     * 특정 가게의 PAYMENT_COMPLETED 주문 목록 조회.
     * 오너 페이지 section-pickup 하단 "入金済み注文リスト" 에 표시.
     * 결제 시각 오래된 순(ASC)으로 정렬 — 대기 시간이 긴 주문을 위로.
     */
    List<PendingPickupOrderDto> findPendingPickupOrders(@Param("storeId") Long storeId);
    
}