package com.deushu.member.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.deushu.esg.dto.EsgForestDto;
import com.deushu.esg.dto.OrderCarbonSavingDto;
import com.deushu.esg.mapper.EsgRepository;
import com.deushu.member.mapper.MemberRepository;
import com.deushu.order.dto.MyPageOrderResponseDto;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageApiController {

	// DI (依存性注入): べさんが作った MemberRepository をそのまま活用します
    private final MemberRepository memberRepository;
    private final EsgRepository esgRepository;

    /*
     * FR-Mypage-01: 本日のピックアップ対象一覧を取得するREST API
     */
    @GetMapping("/orders/today")
    public ResponseEntity<?> getTodayOrders(HttpSession session) {
        
        // 🚨 べさんの AuthController に合わせてセッションキーを "memberId" に統一！
        Long loginMemberId = (Long) session.getAttribute("memberId");

        if (loginMemberId == null) {
            log.warn("❌ 未認証ユーザーのマイページAPI接近要請");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                 .body("ログインが必要です。");
        }

        log.info("🔍 本日のピックアップ注文を照会します。 memberId: {}", loginMemberId);
        
        try {
            // MyPageMapper(MemberRepository -> MemberMapper.xml) を通じてDBから本日の決済完了注文リストを取得
            List<MyPageOrderResponseDto> todayOrders = memberRepository.findTodayPickupOrders(loginMemberId);
            return ResponseEntity.ok(todayOrders);
            
        } catch (Exception e) {
            log.error("❌ マイページデータの取得中にエラーが発生しました: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("サーバーエラーが発生しました。");
        }
    }
    
 // =====================================================================
    // 🚨 [核心] ここが欠落していたため "No mapping" エラーが発生していました！
    // FR-Mypage-02: ユーザーの全注文履歴を取得するAPI
    // =====================================================================
    @GetMapping("/orders/history")
    public ResponseEntity<?> getOrderHistory(HttpSession session) {
        Long loginMemberId = (Long) session.getAttribute("memberId");

        if (loginMemberId == null) {
            log.warn("❌ 未認証ユーザーのマイページAPI接近要請 (History)");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です。");
        }

        log.info("🔍 全注文履歴を照会します。 memberId: {}", loginMemberId);
        
        try {
            // MemberRepository に追加した履歴照会メソッドを呼び出し
            List<MyPageOrderResponseDto> historyOrders = memberRepository.findAllOrderHistory(loginMemberId);
            return ResponseEntity.ok(historyOrders);
            
        } catch (Exception e) {
            log.error("❌ 注文履歴の取得中にエラーが発生しました: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("サーバーエラーが発生しました。");
        }
    }
    

    // ============================================================
    // ★ 추가 메소드 — ESG 숲 시스템
    // ============================================================
 
    /*
     * [추가] FR-ESG-01: 마이페이지 ESG 섹션 데이터 조회
     * GET /api/v1/mypage/esg
     * 인증 필요 (세션)
     * 반환: { cumulativeCarbon, cumulativeDays, treeCount, forestLevel, forestGifPath, forestLevelName, daysUntilNextLevel }
     */
    @GetMapping("/esg")
    public ResponseEntity<?> getEsgForest(HttpSession session) {
        Long loginMemberId = (Long) session.getAttribute("memberId");
 
        if (loginMemberId == null) {
            log.warn("❌ 未認証ユーザーのESGデータ接近要請");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です。");
        }
 
        log.info("🌱 ESGフォレストデータを照会します。 memberId: {}", loginMemberId);
        try {
            EsgForestDto esgForest = esgRepository.findEsgForest(loginMemberId);
 
            // null 방어 (virtual_forests 레코드 자체가 없는 신규 회원 대응)
            if (esgForest == null) {
                esgForest = new EsgForestDto();
                esgForest.setCumulativeCarbon(0.0);
                esgForest.setCumulativeDays(0L);
                esgForest.setTreeCount(0);
                esgForest.setForestLevel(1);
            }
 
            // 편의 필드를 Map으로 직렬화 (편의 메소드는 JSON으로 자동 직렬화되지 않으므로 명시)
            Map<String, Object> result = new HashMap<>();
            result.put("cumulativeCarbon",   esgForest.getCumulativeCarbon());
            result.put("cumulativeDays",      esgForest.getCumulativeDays());
            result.put("treeCount",           esgForest.getTreeCount());
            result.put("forestLevel",         esgForest.getForestLevel());
            result.put("forestGifPath",       esgForest.getForestGifPath());
            result.put("forestLevelName",     esgForest.getForestLevelName());
            result.put("daysUntilNextLevel",  esgForest.getDaysUntilNextLevel());
 
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ ESGデータの取得中にエラーが発生しました: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("サーバーエラーが発生しました。");
        }
    }
 
    /*
     * [추가] FR-ESG-02: 전체 커뮤니티 숲 레벨 조회 (인증 불필요 — index.html 표시용)
     * GET /api/v1/esg/community
     * 반환: { totalLevelSum, communityLevel, communityGifPath }
     */
    @GetMapping("/esg/community")
    public ResponseEntity<?> getCommunityForest() {
        try {
            Integer totalSum = esgRepository.findCommunityForestTotal();
            if (totalSum == null) totalSum = 0;
 
            // 커뮤니티 레벨 결정 (EsgService.calcCommunityLevel 과 동일 로직)
            int communityLevel;
            if      (totalSum >= 500) communityLevel = 5;
            else if (totalSum >= 200) communityLevel = 4;
            else if (totalSum >= 50)  communityLevel = 3;
            else if (totalSum >= 10)  communityLevel = 2;
            else                      communityLevel = 1;
 
            Map<String, Object> result = new HashMap<>();
            result.put("totalLevelSum",    totalSum);
            result.put("communityLevel",   communityLevel);
            result.put("communityGifPath", "/images/esg/community-level" + communityLevel + ".gif");
 
            log.info("🌳 コミュニティフォレスト照会 — totalSum: {}, communityLevel: {}", totalSum, communityLevel);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ コミュニティフォレストの取得中にエラーが発生しました: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("サーバーエラーが発生しました。");
        }
    }
 
    /*
     * [추가] FR-ESG-03: 특정 주문의 탄소 절감량 조회 (결제 완료 모달용)
     * GET /api/v1/orders/{orderId}/carbon
     * 인증 필요 (세션)
     * 반환: { orderId, totalCarbonKg, totalTreeDays, treeDaysMessage }
     */
    @GetMapping("/orders/{orderId}/carbon")
    public ResponseEntity<?> getOrderCarbonSaving(@PathVariable Long orderId, HttpSession session) {
        Long loginMemberId = (Long) session.getAttribute("memberId");
 
        if (loginMemberId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("ログインが必要です。");
        }
 
        try {
            OrderCarbonSavingDto saving = esgRepository.findOrderCarbonSaving(orderId);
 
            if (saving == null) {
                // 픽업 완료 전이거나 아직 계산 전 — 빈 응답 반환
                Map<String, Object> empty = new HashMap<>();
                empty.put("orderId",         orderId);
                empty.put("totalCarbonKg",   0.0);
                empty.put("totalTreeDays",   0);
                empty.put("treeDaysMessage", "約0日分");
                return ResponseEntity.ok(empty);
            }
 
            Map<String, Object> result = new HashMap<>();
            result.put("orderId",         saving.getOrderId());
            result.put("totalCarbonKg",   saving.getTotalCarbonKg());
            result.put("totalTreeDays",   saving.getTotalTreeDays());
            result.put("treeDaysMessage", saving.getTreeDaysMessage());
 
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("❌ 注文タンソデータの取得中にエラーが発生しました: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("サーバーエラーが発生しました。");
        }
    }
}