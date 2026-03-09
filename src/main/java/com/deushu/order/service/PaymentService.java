package com.deushu.order.service;

import com.deushu.order.domain.OrderEntity;
import com.deushu.order.mapper.OrderMapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderMapper orderMapper;
    private final WebClient webClient = WebClient.create("https://api.iamport.kr");

    // application.properties に設定した PortOne V1 API キーを注入
    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    /*
     * FR-P02: PortOneサーバーと通信し、決済金額の改ざんを検証(Validation)します。
     * 正常であれば注文ステータスを 'PAYMENT_COMPLETED' に更新します。
     */
    @Transactional(rollbackFor = Exception.class)
    public void verifyAndCompletePayment(Long orderId, String impUid, Long memberId) {
        
        // 1. DBから決済待機中(PAYMENT_PENDING)の注文情報を照会
        OrderEntity order = orderMapper.findOrderById(orderId);
        if (order == null || !order.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("無効な注文情報、または権限がありません。");
        }
        if (!"PAYMENT_PENDING".equals(order.getOrderStatus())) {
            throw new IllegalStateException("すでに処理された、またはキャンセルされた注文です。");
        }

        // 2. PortOne REST API 用の Access Token を取得 (S2S通信)
        String accessToken = getPortOneAccessToken();

        // 3. 取得したTokenを用いて、imp_uid（決済固有番号）に紐づく実際の決済情報を照会
        JsonNode paymentData = getPaymentDataFromPortOne(impUid, accessToken);

        // 4. 改ざん検証（Cross-Validation）: DBの注文金額 vs 実際の決済金額
        int actualPaidAmount = paymentData.get("amount").asInt();
        if (order.getTotalPrice() != actualPaidAmount) {
            log.error("決済金額の改ざんを検知: DB金額={}, 実際決済金額={}, impUid={}", 
                      order.getTotalPrice(), actualPaidAmount, impUid);
            // TODO: ここでPortOneの決済キャンセルAPIを呼び出し、強制返金処理を行うロジックを追加推奨
            throw new IllegalStateException("決済金額が一致しません。決済は取り消されます。");
        }

        // 5. 検証成功: 注文ステータスを「決済完了」に更新
        order.setOrderStatus("PAYMENT_COMPLETED");
        orderMapper.updateOrderStatus(order);

        // [TODO] 6. (パクさん担当領域) ESGエコポイント付与スケジューラー/ロジックの呼び出し
        // memberMapper.addEsgPoint(memberId, calculatedCarbonReduction);

        log.info("決済検証完了およびステータス更新成功: 注文ID={}", orderId);
    }

    /*
     * PortOne APIにアクセスするためのTokenを発行（内部メソッド）
     */
    private String getPortOneAccessToken() {
        Map<String, String> tokenRequest = new HashMap<>();
        tokenRequest.put("imp_key", apiKey);
        tokenRequest.put("imp_secret", apiSecret);

        JsonNode response = webClient.post()
                .uri("/users/getToken")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(tokenRequest)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(); // トランザクション内での直列処理のため、ここではBlockで同期処理化

        if (response == null || response.get("code").asInt() != 0) {
            throw new RuntimeException("PortOne Access Token の取得に失敗しました。");
        }

        return response.get("response").get("access_token").asText();
    }

    /*
     * impUidを利用して実際の決済データ(JSON)を取得（内部メソッド）
     */
    private JsonNode getPaymentDataFromPortOne(String impUid, String accessToken) {
    	log.info("🔍 検証開始 - 決済番号(imp_uid): {}, Sandboxモード: true", impUid);
    	
    	try {
            JsonNode response = webClient.get()
            		.uri(uriBuilder -> uriBuilder
                            .path("/payments/" + impUid)
                            // 🚨 [核心] 2026年以降、テスト決済の照会にはこのパラメータが必須です。
                            .queryParam("include_sandbox", true) 
                            .build())
                    .header("Authorization", accessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> 
	                    clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
	                        // 🚨 [核心] PortOneサーバーが返す生のJSONエラーメッセージをログに出力
	                        log.error("==== PortOne API Error Detail ====");
	                        log.error("Status: {}", clientResponse.statusCode());
	                        log.error("Body: {}", errorBody);
	                        log.error("===================================");
	                        return Mono.error(new RuntimeException("PortOne API Error: " + errorBody));
	                    	})
                    )
                    .bodyToMono(JsonNode.class)
                    .block();
            
            log.info("PortOne 応答詳細: {}", response);
            
            if (response == null || response.get("code").asInt() != 0) {
                // throw new RuntimeException("PortOne 決済情報の照会に失敗しました。impUid=" + impUid);
            	throw new RuntimeException(" * 決済情報の照会失敗: " + (response != null ? response.get("message") : "応答なし"));
            }

            return response.get("response");
        } catch (WebClientResponseException e) {
            log.error("❌ 決済データの取得中に例外が発生しました: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}