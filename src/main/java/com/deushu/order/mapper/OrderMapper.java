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
}