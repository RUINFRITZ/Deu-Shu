package com.deushu.esg.controller;

import com.deushu.common.response.ApiResponse;
import com.deushu.esg.dto.CarbonPreviewDto;
import com.deushu.esg.service.EsgService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ESG 탄소 절감 REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class EsgController {

    private final EsgService esgService;

    /**
     * 결제 완료 직후 탄소 절감 미리보기 조회
     *
     * 엔드포인트: GET /api/v1/mypage/orders/{orderId}/carbon
     * 호출 시점: detail.js verifyPayment() 성공 직후
     *
     * order_items × stores.category × carbon_emission_factors 를 JOIN 하여
     * 해당 주문에서 절감된 탄소량(kg)과 소나무 환산 일수를 계산합니다.
     *
     * carbon_emission_factors 기본 데이터 (Schema V1.5):
     *   BAKERY   → 0.60 kg / 33일   (소나무 약 1개월분)
     *   SUSHI    → 1.20 kg / 66일   (소나무 약 2개월분)
     *   LUNCHBOX → 2.00 kg / 110일  (소나무 약 4개월분)
     *   CAFE     → 0.40 kg / 22일   (소나무 약 3주분)
     *   SIDEDISH → 0.80 kg / 44일   (소나무 약 1.5개월분)
     *
     * @param orderId 결제 완료된 주문 ID
     * @return CarbonPreviewDto { category, totalCarbonKg, totalTreeDays }
     */
    @GetMapping("/orders/{orderId}/carbon")
    public ApiResponse<CarbonPreviewDto> getCarbonPreview(
            @PathVariable("orderId") Long orderId
    ) {
        log.info("🌿 [ESG] 탄소 절감 미리보기 조회 — orderId: {}", orderId);
        CarbonPreviewDto preview = esgService.calcCarbonPreview(orderId);
        return ApiResponse.onSuccess(preview);
    }
}