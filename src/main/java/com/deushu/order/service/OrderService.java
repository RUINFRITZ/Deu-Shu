package com.deushu.order.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deushu.order.domain.ItemEntity;
import com.deushu.order.domain.OrderEntity;
import com.deushu.order.domain.OrderItemEntity;
import com.deushu.order.dto.MyOrderResponse;
import com.deushu.order.dto.OrderCreateRequestDto;
import com.deushu.order.mapper.OrderMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

        String shortPickupCode = generateShortPickupCode();
        
        // 3. 注文(親)データの生成
        OrderEntity newOrder = OrderEntity.builder()
                .memberId(memberId)
                .storeId(requestDto.getStoreId())
                .orderStatus("PAYMENT_PENDING") // まずは「決済待機」状態で保存
                .totalPrice(calculatedTotalPrice)
                .pickupCode(shortPickupCode)
                .build();
        
        orderMapper.insertOrder(newOrder); // 実行後、newOrder.getId() にPKがセットされる

        // 4. 生成された親のIDを子データにセットし、一括(Bulk)保存
        for (OrderItemEntity oi : orderItems) {
            oi.setOrderId(newOrder.getId());
        }
        orderMapper.insertOrderItems(orderItems);

        log.info("注文生成完了(待機状態): 注文ID={}, ユーザーID={}, 金額={}, ピックアップコード={}", 
                newOrder.getId(), memberId, calculatedTotalPrice, shortPickupCode);
        
        return newOrder.getId();
    }
    
    /*
     * 私の支払い完了 注文リストを確認
     */
    public List<MyOrderResponse> getMyCompletedOrders(Long memberId, Long storeId) {
        return orderMapper.findMyCompletedOrders(memberId, storeId);
    }
    
    // =====================================================================
    // ヘルパーメソッド: 人間が読みやすい(Human-readable)ピックアップコードを生成
    // フォーマット: yyMMdd-XXXX-mmss (例: 260311-8492-4530)
    // 万が一QRスキャナーが故障した際も、店舗スタッフが容易に手入力できる設計です。
    // =====================================================================
    private String generateShortPickupCode() {
        LocalDateTime now = LocalDateTime.now();
        
        // 1. 本日の日付 (MMdd) - 6桁
        String datePart = now.format(DateTimeFormatter.ofPattern("yyMMdd"));
        
        // 2. ランダムな数字 - 4桁 (1000 ~ 9999)
        int randomPart = ThreadLocalRandom.current().nextInt(1000, 10000);
        
        // 3. 現在の分と秒 (mmss) - 4桁
        String timePart = now.format(DateTimeFormatter.ofPattern("mmss"));
        
        // 🚨 UX向上のため、ハイフン(-)で区切り視認性を高めます
        return String.format("%s-%04d-%s", datePart, randomPart, timePart);
    }
    
    /*
     * 過去の失敗した注文情報を元に、新たな注文を生成(Clone)して在庫を再確保します。
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> recreateOrderFromFailed(Long memberId, Long oldOrderId) {
        
        OrderEntity oldOrder = orderMapper.findOrderById(oldOrderId);
        if (oldOrder == null || !oldOrder.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("無効な注文情報です。");
        }
        
        // 自分が握っている在庫(PAYMENT_PENDING)なら、先に解放(Rollback)して自己競合を防ぐ
        if ("PAYMENT_PENDING".equals(oldOrder.getOrderStatus())) {
            log.info("決済中断状態からの再決済。旧注文(ID:{})の在庫を一時解放します。", oldOrderId);
            oldOrder.setOrderStatus("CANCELED"); 
            orderMapper.updateOrderStatus(oldOrder);
            orderMapper.restoreItemStock(oldOrderId); // 自分のトランザクション内で在庫を即時復元
        } else if ("PAYMENT_COMPLETED".equals(oldOrder.getOrderStatus()) || "PICKED_UP".equals(oldOrder.getOrderStatus())) {
            throw new IllegalStateException("既に決済が完了している注文です。");
        }
        
        List<OrderItemEntity> oldItems = orderMapper.findOrderItemsByOrderId(oldOrderId);
        int calculatedTotalPrice = 0;
        List<OrderItemEntity> newOrderItems = new ArrayList<>();

        // 1. 旧注文の商品をループし、現在の在庫状態を悲観的ロックで再チェック
        for (OrderItemEntity oldItem : oldItems) {
            ItemEntity item = orderMapper.findItemByIdWithPessimisticLock(oldItem.getItemId());
            
            if (item == null || item.getDeletedAt() != null) {
                throw new IllegalArgumentException("販売が終了した商品が含まれているため、再決済できません。");
            }
            if (item.getStock() < oldItem.getQuantity()) {
                throw new IllegalArgumentException("在庫が不足している商品があるため、再決済できません。");
            }

            // 在庫の再確保 (removeStock)
            item.removeStock(oldItem.getQuantity());
            orderMapper.updateItemStock(item);

            calculatedTotalPrice += (item.getDiscountPrice() * oldItem.getQuantity());

            newOrderItems.add(OrderItemEntity.builder()
                    .itemId(item.getId())
                    .quantity(oldItem.getQuantity())
                    .orderPrice(item.getDiscountPrice())
                    .build());
        }

        // 2. 新しい注文の生成 (Short UUID)
        String shortPickupCode = generateShortPickupCode();
        OrderEntity newOrder = OrderEntity.builder()
                .memberId(memberId)
                .storeId(oldOrder.getStoreId())
                .orderStatus("PAYMENT_PENDING")
                .totalPrice(calculatedTotalPrice)
                .pickupCode(shortPickupCode)
                .build();
        
        orderMapper.insertOrder(newOrder);

        for (OrderItemEntity noi : newOrderItems) {
            noi.setOrderId(newOrder.getId());
        }
        orderMapper.insertOrderItems(newOrderItems);

        // 3. フロントエンドのPortOne決済に必要な情報を返却
        Map<String, Object> result = new HashMap<>();
        result.put("orderId", newOrder.getId());
        result.put("totalPrice", calculatedTotalPrice);
        return result;
    }
}