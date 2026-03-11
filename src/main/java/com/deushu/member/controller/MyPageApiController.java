package com.deushu.member.controller;

import com.deushu.member.mapper.MemberRepository;
import com.deushu.order.dto.MyPageOrderResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpSession;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/mypage")
@RequiredArgsConstructor
public class MyPageApiController {

	// DI (依存性注入): べさんが作った MemberRepository をそのまま活用します
    private final MemberRepository memberRepository;

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
}