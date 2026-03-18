package com.deushu.order.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.deushu.esg.service.EsgService;
import com.deushu.order.domain.OrderEntity;
import com.deushu.order.dto.OrderQrDto;
import com.deushu.order.dto.PickupVerifyResponse;
import com.deushu.order.mapper.OrderMapper;
import com.deushu.store.domain.StoreEntity;
import com.deushu.store.mapper.StoreMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * QR 코드 조회(고객) + 픽업 확인(오너) 전용 서비스.
 * 기존 OrderService / PaymentService 는 일절 수정하지 않음.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQrService {

    private final OrderMapper orderMapper;
    private final StoreMapper storeMapper;
    private final EsgService esgService;
    // ── 고객: QR 코드 정보 조회 ──────────────────────────────────────

    /**
     * GET /api/v1/orders/{orderId}/qr
     * 본인 주문만 조회 (memberId 검증).
     */
    public OrderQrDto getOrderQr(Long orderId, Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "ログインが必要です。");
        }

        // OrderMapper의 기존 findOrderById 재사용
        OrderEntity order = orderMapper.findOrderById(orderId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "注文が見つかりません。");
        }
        if (!order.getMemberId().equals(memberId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "アクセス権限がありません。");
        }

        // 가게 이름 조회 (StoreMapper 기존 메서드 재사용)
        StoreEntity store = storeMapper.findStoreById(order.getStoreId());
        String storeName = (store != null) ? store.getName() : null;

        return OrderQrDto.builder()
                .orderId(order.getId())
                .pickupCode(order.getPickupCode())
                .orderStatus(order.getOrderStatus())
                .totalPrice(order.getTotalPrice())
                .storeName(storeName)
                .createdAt(order.getCreatedAt() != null
                        ? order.getCreatedAt().toString().substring(0, 16).replace('T', ' ')
                        : null)
                .build();
    }

    // ── 오너: pickupCode로 주문 조회 ─────────────────────────────────

    /**
     * GET /api/owner/pickup/{pickupCode}
     * 오너 본인 가게의 주문인지 storeId로 검증.
     */
    public PickupVerifyResponse verifyPickup(String pickupCode, Long ownerId) {
        // 1. 오너의 가게 조회
        StoreEntity store = storeMapper.findByOwnerId(ownerId);
        if (store == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "店舗情報が見つかりません。");
        }

        // 2. pickupCode로 주문 조회 (신규 쿼리)
        OrderEntity order = orderMapper.findOrderByPickupCode(pickupCode);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "該当する注文が見つかりません。");
        }

        // 3. 본인 가게 주문인지 검증
        if (!order.getStoreId().equals(store.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "他店舗の注文です。");
        }

        // 4. 주문 상품 수 조회 (신규 쿼리)
        int itemCount = orderMapper.countOrderItems(order.getId());

        return PickupVerifyResponse.builder()
                .orderId(order.getId())
                .orderStatus(order.getOrderStatus())
                .totalPrice(order.getTotalPrice())
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt() != null
                        ? order.getCreatedAt().toString().substring(0, 16).replace('T', ' ')
                        : null)
                .storeId(store.getId())
                .build();
    }

    // ── 오너: 픽업 완료 처리 ─────────────────────────────────────────

    /**
     * PATCH /api/owner/pickup/{pickupCode}/complete
     * PAYMENT_COMPLETED → PICKUP_COMPLETED 상태 변경.
     */
    @Transactional
    public void completePickup(String pickupCode, Long ownerId) {
        // 가게 소유 검증
        StoreEntity store = storeMapper.findByOwnerId(ownerId);
        if (store == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "店舗情報が見つかりません。");
        }

        OrderEntity order = orderMapper.findOrderByPickupCode(pickupCode);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "該当する注文が見つかりません。");
        }
        if (!order.getStoreId().equals(store.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "他店舗の注文です。");
        }
        if (!"PAYMENT_COMPLETED".equals(order.getOrderStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ピックアップ可能な状態ではありません。現在のステータス: " + order.getOrderStatus());
        }

        // 상태 변경 (신규 쿼리: pickupCode 기반)
        orderMapper.updateStatusByPickupCode(pickupCode, "PICKUP_COMPLETED");
        log.info("[PickupComplete] pickupCode={} → PICKUP_COMPLETED", pickupCode);
        
        // ★ 4. ESG 탄소 절감 처리
        //      order_carbon_savings 저장 + virtual_forests 누적 업데이트
        //      order.getMemberId() 는 주문자(고객) ID
        try {
            esgService.processPickupCompletion(order.getId(), order.getMemberId());
        } catch (Exception e) {
            // ESG 처리 실패가 픽업 완료 자체를 롤백시키지 않도록 로그만 기록
            // (탄소 데이터는 보조 기능 — 픽업 완료가 핵심 트랜잭션)
            log.error("[ESG] 탄소 절감 처리 실패 — orderId: {}, memberId: {} | {}",
                    order.getId(), order.getMemberId(), e.getMessage());
        }
    }
}