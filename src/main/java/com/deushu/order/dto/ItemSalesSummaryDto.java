package com.deushu.order.dto;

import lombok.Data;

/**
 * 품목별 매출 집계 DTO
 * 매출 정산 페이지의 TOP3 품목 및 원형 그래프용
 */
@Data
public class ItemSalesSummaryDto {
    private String itemName;       // 품목명
    private int    totalQuantity;  // 총 판매 수량
    private long   totalRevenue;   // 총 매출액 (결제완료 기준)
}