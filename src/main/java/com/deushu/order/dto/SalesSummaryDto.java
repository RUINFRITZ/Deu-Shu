package com.deushu.order.dto;


import lombok.Data;

import java.time.LocalDate;

/**
 * 매출 정산 집계 데이터 DTO
 * orders 테이블 기반으로 집계된 오너 전용 매출 요약 정보
 */
@Data
public class SalesSummaryDto {

    // ── 전체 집계 ────────────────────────────────
    /** 결제 완료 총 매출액 */
    private int totalRevenue;

    /** 전체 주문 건수 */
    private int totalOrders;

    /** 픽업 완료 건수 */
    private int pickupCompleted;

    /** 결제 대기 건수 */
    private int paymentPending;

    /** 취소 건수 */
    private int canceled;

    /** 기간 만료 건수 */
    private int expired;

    // ── 일별 집계 (차트 렌더링용) ─────────────────
    /** 날짜 (yyyy-MM-dd) */
    private LocalDate orderDate;

    /** 일별 결제 완료 매출 */
    private int dailyRevenue;

    /** 일별 주문 건수 */
    private int dailyCount;

    /** 일별 결제 완료 건수 (테이블 컬럼용) */
    private int paymentCompleted;
}