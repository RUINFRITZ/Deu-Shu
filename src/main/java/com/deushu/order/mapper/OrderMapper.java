package com.deushu.order.mapper;

import com.deushu.order.domain.ItemEntity;
import com.deushu.order.domain.OrderEntity;
import com.deushu.order.domain.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

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

    /*
     * 인덱스 페이지에서 히어로 버튼 분기에 사용
     * 해당 회원의 오늘 결제 완료(PAYMENT_COMPLETED) 주문 수를 반환
     *
     * @param memberId 로그인한 회원 ID
     * @return 오늘 완료된 주문 수 (0이면 주문 없음, 1 이상이면 주문 있음)
     */
    int countTodayCompletedOrders(@Param("memberId") Long memberId);
}