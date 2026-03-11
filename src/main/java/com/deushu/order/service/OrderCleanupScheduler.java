// =========================================================================
// [ ドゥーシュー ] OrderCleanupScheduler.java
// 一定間隔でDBをポーリングし、決済されずに放置された注文(Dead Stock)を解放します。
// =========================================================================

package com.deushu.order.service;

import com.deushu.order.domain.OrderEntity;
import com.deushu.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCleanupScheduler {

    private final OrderMapper orderMapper;

    /*
     * 毎分0秒に実行されるクーロン(Cron)ジョブ
     * 5分以上「決済待機(PAYMENT_PENDING)」状態の注文を検索し、在庫をロールバックします。
     */
    @Scheduled(cron = "0 * * * * *") // 毎分実行 (0초에 시작)
    @Transactional(rollbackFor = Exception.class)
    public void cleanupExpiredOrders() {
        log.info("⏳ [Scheduler] 未決済注文のクリーンアップ処理を開始します...");

        // 1. 5分経過した未決済注文のリストを取得
        int expirationMinutes = 5;
        List<OrderEntity> expiredOrders = orderMapper.findPendingOrdersOlderThan(expirationMinutes);

        if (expiredOrders.isEmpty()) {
            return; // 対象がなければ終了
        }

        // 2. 対象となる注文をループ処理で一括ロールバック
        for (OrderEntity order : expiredOrders) {
            log.warn("🚨 決済タイムアウト(5分経過)により、注文を自動キャンセルします。注文ID: {}", order.getId());

            // 2-1. 注文ステータスを 'EXPIRED' (または 'CANCELED') に更新
            order.setOrderStatus("EXPIRED");
            orderMapper.updateOrderStatus(order);

            // 2-2. 確保していた在庫(Stock)を元の店舗に返却 (Compensation Transaction)
            orderMapper.restoreItemStock(order.getId());
            
            log.info("✅ 注文ID: {} の在庫ロールバックが完了しました。", order.getId());
        }
    }
}