package com.deushu.order.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deushu.common.response.ApiResponse;
import com.deushu.order.dto.OrderCreateRequestDto;
import com.deushu.order.dto.PaymentVerifyRequestDto;
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
}