package com.deushu.esg.dto;

import lombok.Data;

/**
 * 결제 완료 직후 탄소 절감 모달 표시용 DTO
 *
 * order_carbon_savings 는 PICKUP_COMPLETED 시에만 저장되므로,
 * 결제 완료(PAYMENT_COMPLETED) 시점에는 order_items × carbon_emission_factors 를
 * 동적으로 계산하여 미리보기(preview) 값으로 모달에 표시합니다.
 *
 * carbon_emission_factors 기본 데이터 (Schema V1.5):
 *   BAKERY   → 0.60 kg, 33일
 *   SUSHI    → 1.20 kg, 66일
 *   LUNCHBOX → 2.00 kg, 110일
 *   CAFE     → 0.40 kg, 22일
 *   SIDEDISH → 0.80 kg, 44일
 */
@Data
public class CarbonPreviewDto {

    /** stores.category (DB ENUM): BAKERY / SUSHI / LUNCHBOX / CAFE / SIDEDISH */
    private String  category;

    /** 해당 주문으로 절감되는 총 탄소량 (kg CO2e) */
    private Double  totalCarbonKg;

    /** 소나무 환산 일수 */
    private Integer totalTreeDays;

    /**
     * "約XX日分" / "約X年分" 형식 텍스트 반환 (프론트 표시용)
     */
    public String getTreeDaysMessage() {
        if (totalTreeDays == null || totalTreeDays == 0) return "約0日分";
        if (totalTreeDays >= 365) {
            int years  = totalTreeDays / 365;
            int remain = totalTreeDays % 365;
            return remain == 0
                ? "約" + years + "年分"
                : "約" + years + "年" + remain + "日分";
        }
        return "約" + totalTreeDays + "日分";
    }
}