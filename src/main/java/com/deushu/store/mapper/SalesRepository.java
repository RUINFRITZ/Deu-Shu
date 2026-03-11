package com.deushu.store.mapper;


import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}