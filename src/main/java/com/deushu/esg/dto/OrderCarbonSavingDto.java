package com.deushu.esg.dto;

import lombok.Data;

/**
 * 결제 완료 후 탄소 절감 모달 표시용 DTO
 * order_carbon_savings 테이블 조회 결과 매핑
 */
@Data
public class OrderCarbonSavingDto {

    private Long    orderId;         // 주문 ID
    private Double  totalCarbonKg;   // 해당 주문으로 절감한 탄소량 (kg CO2e)
    private Integer totalTreeDays;   // 소나무 환산 일수

    /**
     * "約XX日分" 형식의 소나무 기여 텍스트 반환 (프론트 표시용 편의 메소드)
     */
    public String getTreeDaysMessage() {
        if (totalTreeDays == null || totalTreeDays == 0) return "約0日分";
        if (totalTreeDays >= 365) {
            int years = totalTreeDays / 365;
            int remain = totalTreeDays % 365;
            return remain == 0
                ? "約" + years + "年分"
                : "約" + years + "年" + remain + "日分";
        }
        return "約" + totalTreeDays + "日分";
    }
}