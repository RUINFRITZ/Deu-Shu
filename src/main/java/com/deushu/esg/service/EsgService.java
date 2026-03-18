package com.deushu.esg.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.deushu.esg.dto.CarbonPreviewDto;
import com.deushu.esg.dto.EsgForestDto;
import com.deushu.esg.mapper.EsgRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ESG 탄소 절감 핵심 비즈니스 로직
 *
 * 픽업 완료(PICKUP_COMPLETED) 시 호출되어:
 *  1. 주문별 탄소 절감량 계산 → order_carbon_savings 저장
 *  2. virtual_forests 누적값 갱신
 *  3. cumulative_days >= 365 이면 forest_level 자동 증가
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsgService {

    private final EsgRepository esgRepository;

    /**
     * 픽업 완료 이벤트 처리 — 주문의 탄소 절감량 계산 후 저장
     *
     * 호출 위치: OwnerPickupController 또는 OrderService 의 픽업 완료 처리 메소드에서
     * 사용 예시:
     *   esgService.processPickupCompletion(orderId, memberId);
     *
     * @param orderId  방금 픽업 완료된 주문 ID
     * @param memberId 해당 주문의 회원 ID
     */
    @Transactional
    public void processPickupCompletion(Long orderId, Long memberId) {
        log.info("🌱 [ESG] 픽업 완료 탄소 절감 처리 시작 — orderId: {}, memberId: {}", orderId, memberId);

        // 1. 해당 주문의 탄소 절감량 계산
        //    order_items × stores.category → carbon_emission_factors.carbon_kg 와 tree_days 합산
        Double totalCarbonKg = esgRepository.calcOrderCarbonKg(orderId);
        Integer totalTreeDays = esgRepository.calcOrderTreeDays(orderId);

        if (totalCarbonKg == null || totalCarbonKg == 0.0) {
            log.warn("⚠️ [ESG] orderId {} 의 탄소 절감량이 0입니다. carbon_emission_factors 데이터를 확인하세요.", orderId);
            return;
        }

        // 2. order_carbon_savings 에 저장 (이미 존재하면 중복 저장 방지)
        esgRepository.insertOrderCarbonSaving(orderId, memberId, totalCarbonKg, totalTreeDays);
        log.info("✅ [ESG] order_carbon_savings 저장 완료 — {}kg, {}일", totalCarbonKg, totalTreeDays);

        // 3. virtual_forests 테이블 없으면 초기 레코드 생성 (신규 회원 대응)
        esgRepository.initVirtualForestIfAbsent(memberId);

        // 4. cumulative_carbon / cumulative_days 누적 업데이트
        esgRepository.updateVirtualForestCumulative(memberId, totalCarbonKg, totalTreeDays);

        // 5. forest_level 재계산 — cumulative_days / 365 초과할 때마다 +1 레벨
        //    레벨은 최대 5까지 제한
        Long newCumulativeDays = esgRepository.getCumulativeDays(memberId);
        int newTreeCount = (int)(newCumulativeDays / 365);
        int newForestLevel = Math.min(newTreeCount + 1, 5); // 레벨 1 = 0그루, 레벨 2 = 1그루...

        esgRepository.updateForestLevelAndTreeCount(memberId, newTreeCount, newForestLevel);
        log.info("🌳 [ESG] 숲 레벨 갱신 완료 — level: {}, treeCount: {}", newForestLevel, newTreeCount);
    }

    /**
     * 마이페이지 ESG 섹션 데이터 조회
     */
    public EsgForestDto getEsgForest(Long memberId) {
        return esgRepository.findEsgForest(memberId);
    }

    /**
     * 전체 회원의 forest_level 합 조회 (index.html 커뮤니티 숲 표시용)
     */
    public Integer getCommunityForestTotal() {
        return esgRepository.findCommunityForestTotal();
    }

    /**
     * forest_level 합계로 커뮤니티 숲 레벨 결정 (1~5)
     *  - 0~9   : level 1 (씨앗 숲)
     *  - 10~49 : level 2 (묘목 숲)
     *  - 50~199: level 3 (작은 숲)
     *  - 200~499: level 4 (숲)
     *  - 500+  : level 5 (대형 숲)
     */
    public int calcCommunityLevel(int totalLevelSum) {
        if (totalLevelSum >= 500) return 5;
        if (totalLevelSum >= 200) return 4;
        if (totalLevelSum >= 50)  return 3;
        if (totalLevelSum >= 10)  return 2;
        return 1;
    }
    
    
    /**
     * 결제 완료 직후 탄소 절감 미리보기 계산
     *
     * order_carbon_savings 는 PICKUP_COMPLETED 시에만 저장되므로,
     * PAYMENT_COMPLETED 시점에는 order_items × carbon_emission_factors 를 동적으로 계산합니다.
     *
     * carbon_emission_factors 기본 데이터:
     *   BAKERY   → 0.60 kg / 33일
     *   SUSHI    → 1.20 kg / 66일
     *   LUNCHBOX → 2.00 kg / 110일
     *   CAFE     → 0.40 kg / 22일
     *   SIDEDISH → 0.80 kg / 44일
     *
     * @param orderId 결제 완료된 주문 ID
     * @return CarbonPreviewDto (category, totalCarbonKg, totalTreeDays)
     */
    public CarbonPreviewDto calcCarbonPreview(Long orderId) {
        CarbonPreviewDto preview = esgRepository.calcOrderCarbonPreview(orderId);
        if (preview == null) {
            // carbon_emission_factors 미설정 또는 주문 없음 — 빈 DTO 반환
            CarbonPreviewDto empty = new CarbonPreviewDto();
            empty.setCategory("LUNCHBOX");
            empty.setTotalCarbonKg(0.0);
            empty.setTotalTreeDays(0);
            return empty;
        }
        return preview;
    }
}