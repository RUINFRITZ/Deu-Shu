package com.deushu.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deushu.common.response.ApiResponse;
import com.deushu.member.mapper.MemberRepository;
import com.deushu.order.dto.MyOrderResponse;
import com.deushu.order.dto.OrderCreateRequestDto;
import com.deushu.order.dto.OrderQrDto;
import com.deushu.order.dto.PaymentVerifyRequestDto;
import com.deushu.order.service.OrderQrService;
import com.deushu.order.service.OrderService;
import com.deushu.order.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

	// finalが宣言されているため、必ずコンストラクタで初期化される必要があります。
    // @RequiredArgsConstructor が裏側で自動的にコンストラクタを生成してくれます。
    private final OrderService orderService;
    private final PaymentService paymentService; // PortOne API通信を担当するサービス
    private final OrderQrService orderQrService;
    private final MemberRepository memberRepository;
    /*
     * FR-P01: 注文生成（決済待機状態）および在庫仮確保(Pessimistic Lock) API
     * エンドポイント: POST /api/v1/orders
     * 権限: ROLE_USER (SecurityConfigにて設定済み)
     */
    @PostMapping
    public ApiResponse<Long> createOrder(	
            @Valid @RequestBody OrderCreateRequestDto requestDto,
            @AuthenticationPrincipal Long memberId // SecurityConfig経由で注入されるユーザーPK
    ) {
        log.info("注文リクエスト受信: ユーザーID={}, ペイロード={}", memberId, requestDto);

        Long orderId = orderService.createPendingOrder(memberId, requestDto);
        return ApiResponse.onSuccess(orderId);
    }
    
    /*
     * FR-P02: 決済完了時のサーバー間(S2S)検証およびステータス更新API
     * エンドポイント: POST /api/v1/orders/{orderId}/payment
     */
    @PostMapping("/{orderId}/payment")
    public ApiResponse<String> verifyPayment(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody PaymentVerifyRequestDto requestDto,
            @AuthenticationPrincipal Long memberId
    ) {
        log.info("決済検証リクエスト受信: 注文ID={}, impUid={}", orderId, requestDto.getImpUid());

        paymentService.verifyAndCompletePayment(orderId, requestDto.getImpUid(), memberId);

        return ApiResponse.onSuccess("決済の検証が完了し、注文が確定しました。");
    }
    
    
    // ════════════════════════════════════════════════════════════════
    // ★ 신규 — QR 코드 조회 (고객용)
    // GET /api/v1/orders/{orderId}/qr
    // ════════════════════════════════════════════════════════════════

    /**
     * 결제 완료 후 고객이 QR 페이지에서 호출.
     * pickupCode + 주문 요약 정보 반환.
     *
     * 기존 createOrder/verifyPayment 는 @AuthenticationPrincipal Long memberId 를 사용.
     * 여기서는 UserDetails → username → DB 조회 방식으로 memberId 추출.
     * (프로젝트에 CustomUserDetails 없을 때의 대안)
     */
    @GetMapping("/{orderId}/qr")
    public ResponseEntity<ApiResponse<OrderQrDto>> getOrderQr(
            @PathVariable("orderId") Long orderId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = extractMemberIdFromPrincipal(userDetails);
        if (memberId == null) return ResponseEntity.status(401).build();

        OrderQrDto dto = orderQrService.getOrderQr(orderId, memberId);
        return ResponseEntity.ok(ApiResponse.onSuccess(dto));
    }

    /**
     * UserDetails.getUsername() → loginId → MemberMapper.findIdByLoginId() → memberId
     *
     * ★ MemberMapper에 없다면 추가:
     *   @Select("SELECT id FROM members WHERE login_id = #{loginId} AND deleted_at IS NULL")
     *   Long findIdByLoginId(@Param("loginId") String loginId);
     */
    private Long extractMemberIdFromPrincipal(UserDetails userDetails) {
        if (userDetails == null) return null;
        return memberRepository.findIdByLoginId(userDetails.getUsername());
    }
    /*
     * 내 결제 완료 주문 목록 조회 (리뷰 작성용)
     * GET /api/v1/orders/my?storeId=1
     */
    @GetMapping("/my")
    public ApiResponse<List<MyOrderResponse>> getMyOrders(
            @RequestParam("storeId") Long storeId,
            @AuthenticationPrincipal Long memberId
    ) {
        List<MyOrderResponse> orders = orderService.getMyCompletedOrders(memberId, storeId);
        return ApiResponse.onSuccess(orders);
    }
    
    /*
     * FR-P03: キャンセル・期限切れ注文の再決済(クローン) API
     */
    @PostMapping("/{orderId}/reorder")
    public ApiResponse<Map<String, Object>> reorder(
            @PathVariable("orderId") Long oldOrderId,
            @AuthenticationPrincipal Long memberId
    ) {
        log.info("再決済リクエスト受信: 旧注文ID={}, ユーザーID={}", oldOrderId, memberId);
        
        // 新しい注文IDと金額をMapで返却 (PortOne連携用)
        Map<String, Object> newOrderData = orderService.recreateOrderFromFailed(memberId, oldOrderId);
        return ApiResponse.onSuccess(newOrderData);
    }
}