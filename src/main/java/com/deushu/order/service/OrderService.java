package com.deushu.order.service;

import com.deushu.order.domain.ItemEntity;
import com.deushu.order.domain.OrderEntity;
import com.deushu.order.domain.OrderItemEntity;
import com.deushu.order.dto.OrderCreateRequestDto;
import com.deushu.order.mapper.OrderMapper;
import com.deushu.order.dto.MyOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;

    /*
     * FR-P01: 注文生成および在庫仮確保（悲観的ロック適用）
     */
    @Transactional(rollbackFor = Exception.class) // エラー時は全て無かったことにする(Rollback)
    public Long createPendingOrder(Long memberId, OrderCreateRequestDto requestDto) {
        
        int calculatedTotalPrice = 0;
        List<OrderItemEntity> orderItems = new ArrayList<>();

        // 1. カート内の商品を一つずつ処理 (在庫チェック & 排他制御)
        for (OrderCreateRequestDto.CartItemDto cartItem : requestDto.getCartItems()) {
            
            // [核心] 他のトランザクションが同時にこの商品を買おうとした場合、ここで待機(Lock)させます。
            ItemEntity item = orderMapper.findItemByIdWithPessimisticLock(cartItem.getItemId());
            
            if (item == null || item.getDeletedAt() != null) {
                throw new IllegalArgumentException("存在しない、または販売終了した商品が含まれています。");
            }
            if (!item.getStoreId().equals(requestDto.getStoreId())) {
                throw new IllegalArgumentException("他の店舗の商品が混ざっています。");
            }

            // DDD: エンティティに在庫減少を命令 (足りない場合は例外が発生しRollbackされる)
            item.removeStock(cartItem.getQuantity());
            
            // 減少した在庫をDBに即時反映
            orderMapper.updateItemStock(item);

            // スナップショット価格で小計を計算
            calculatedTotalPrice += (item.getDiscountPrice() * cartItem.getQuantity());

            // 注文詳細(子)エンティティの生成
            orderItems.add(OrderItemEntity.builder()
                    .itemId(item.getId())
                    .quantity(cartItem.getQuantity())
                    .orderPrice(item.getDiscountPrice()) // 現在の割引価格をスナップショットとして保存
                    .build());
        }

        // 2. フロントエンドの計算金額とサーバーの計算金額をクロスチェック(改ざん防止)
        if (calculatedTotalPrice != requestDto.getTotalPrice()) {
            throw new IllegalStateException("決済金額が一致しません。画面を更新して再度お試しください。");
        }

        // 3. 注文(親)データの生成
        OrderEntity newOrder = OrderEntity.builder()
                .memberId(memberId)
                .storeId(requestDto.getStoreId())
                .orderStatus("PAYMENT_PENDING") // まずは「決済待機」状態で保存
                .totalPrice(calculatedTotalPrice)
                .pickupCode(UUID.randomUUID().toString()) // 受け取り用のユニークなUUID生成
                .build();
        
        orderMapper.insertOrder(newOrder); // 実行後、newOrder.getId() にPKがセットされる

        // 4. 生成された親のIDを子データにセットし、一括(Bulk)保存
        for (OrderItemEntity oi : orderItems) {
            oi.setOrderId(newOrder.getId());
        }
        orderMapper.insertOrderItems(orderItems);

        log.info("注文生成完了(待機状態): 注文ID={}, ユーザーID={}, 金額={}", newOrder.getId(), memberId, calculatedTotalPrice);
        
        return newOrder.getId();
    }
    
    /*
     * 내 결제 완료 주문 목록 조회
     */
    public List<MyOrderResponse> getMyCompletedOrders(Long memberId, Long storeId) {
        return orderMapper.findMyCompletedOrders(memberId, storeId);
    }
    
}