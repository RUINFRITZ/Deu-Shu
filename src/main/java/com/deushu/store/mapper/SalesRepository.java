package com.deushu.store.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deushu.order.dto.ItemSalesSummaryDto;
import com.deushu.order.dto.SalesSummaryDto;

import java.util.List;

/**
 * orders 테이블 기반 매출 집계 MyBatis 매퍼 인터페이스
 * SalesMapper.xml 의 SQL 과 1:1 대응
 */
@Mapper
public interface SalesRepository {

    /** 가게 전체 매출 집계 (PAYMENT_COMPLETED 기준) */
    SalesSummaryDto findTotalSummary(Long storeId);

    /**
     * 최근 N일 일별 매출 집계
     * @param storeId 조회할 가게 ID
     * @param days    조회할 일수 (기본 30일)
     */
    List<SalesSummaryDto> findDailySummary(
            @Param("storeId") Long storeId,
            @Param("days")    int days);

    // ── 날짜 범위 기반 신규 쿼리 ──────────────────────────────────────

    /**
     * 날짜 범위 기준 전체 매출 집계
     * @param storeId   가게 ID
     * @param startDate 시작일 (yyyy-MM-dd)
     * @param endDate   종료일 (yyyy-MM-dd)
     */
    SalesSummaryDto findSummaryByRange(
            @Param("storeId")    Long   storeId,
            @Param("startDate")  String startDate,
            @Param("endDate")    String endDate);

    /**
     * 날짜 범위 기준 일별 매출 집계 (결제완료건수 포함)
     * @param storeId   가게 ID
     * @param startDate 시작일 (yyyy-MM-dd)
     * @param endDate   종료일 (yyyy-MM-dd)
     */
    List<SalesSummaryDto> findDailySummaryByRange(
            @Param("storeId")    Long   storeId,
            @Param("startDate")  String startDate,
            @Param("endDate")    String endDate);

    /**
     * 특정 날짜의 품목별 매출 집계 (아코디언 표시용)
     * @param storeId 가게 ID
     * @param date    조회 날짜 (yyyy-MM-dd)
     */
    List<ItemSalesSummaryDto> findDailyItems(
            @Param("storeId") Long   storeId,
            @Param("date")    String date);

    /**
     * 날짜 범위 기준 품목별 매출 집계 (TOP3 + 원형 그래프용)
     * @param storeId   가게 ID
     * @param startDate 시작일 (yyyy-MM-dd)
     * @param endDate   종료일 (yyyy-MM-dd)
     */
    List<ItemSalesSummaryDto> findItemSummaryByRange(
            @Param("storeId")    Long   storeId,
            @Param("startDate")  String startDate,
            @Param("endDate")    String endDate);
}