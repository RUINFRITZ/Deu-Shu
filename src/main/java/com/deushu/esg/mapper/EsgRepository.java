package com.deushu.esg.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deushu.esg.dto.CarbonPreviewDto;
import com.deushu.esg.dto.EsgForestDto;
import com.deushu.esg.dto.OrderCarbonSavingDto;

/**
 * ESG 탄소 절감 전용 MyBatis Mapper
 * SQL 은 EsgMapper.xml 에 정의
 */
@Mapper
public interface EsgRepository {

    // ============================================================
    // 마이페이지 ESG 섹션
    // ============================================================

    /** 특정 회원의 virtual_forests 정보 조회 */
    EsgForestDto findEsgForest(@Param("memberId") Long memberId);

    // ============================================================
    // index.html 커뮤니티 숲
    // ============================================================

    /** 전체 회원의 forest_level 합 반환 */
    Integer findCommunityForestTotal();

    // ============================================================
    // 결제 완료 모달 (detail.html)
    // ============================================================

    /** 주문별 탄소 절감 이력 조회 */
    OrderCarbonSavingDto findOrderCarbonSaving(@Param("orderId") Long orderId);

    // ============================================================
    // 픽업 완료 처리 (EsgService 에서 호출)
    // ============================================================

    /** 주문 상품 × 카테고리 계수 기준 총 탄소량 계산 */
    Double calcOrderCarbonKg(@Param("orderId") Long orderId);

    /** 주문 상품 × 카테고리 계수 기준 총 소나무 일수 계산 */
    Integer calcOrderTreeDays(@Param("orderId") Long orderId);

    /** order_carbon_savings 에 절감 이력 저장 */
    int insertOrderCarbonSaving(
        @Param("orderId")        Long    orderId,
        @Param("memberId")       Long    memberId,
        @Param("totalCarbonKg")  Double  totalCarbonKg,
        @Param("totalTreeDays")  Integer totalTreeDays
    );

    /** virtual_forests 레코드가 없는 신규 회원에게 초기 레코드 생성 */
    void initVirtualForestIfAbsent(@Param("memberId") Long memberId);

    /** cumulative_carbon / cumulative_days 누적 업데이트 */
    void updateVirtualForestCumulative(
        @Param("memberId")      Long    memberId,
        @Param("carbonKg")      Double  carbonKg,
        @Param("treeDays")      Integer treeDays
    );

    /** 완성 소나무 수 및 숲 레벨 갱신 */
    void updateForestLevelAndTreeCount(
        @Param("memberId")    Long    memberId,
        @Param("treeCount")   int     treeCount,
        @Param("forestLevel") int     forestLevel
    );

    /** 현재 누적 소나무 일수 조회 (레벨 계산용) */
    Long getCumulativeDays(@Param("memberId") Long memberId);
    
    /**
     * 결제 완료 직후 탄소 절감 미리보기 계산
     * order_items × stores.category × carbon_emission_factors 동적 JOIN
     * → category / totalCarbonKg / totalTreeDays 를 한 번에 반환
     * (order_carbon_savings 저장 전 PAYMENT_COMPLETED 시점에 호출)
     */
    CarbonPreviewDto calcOrderCarbonPreview(@Param("orderId") Long orderId);

}