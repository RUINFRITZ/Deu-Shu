package com.deushu.store.controller;

import com.deushu.store.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FavoriteController — 즐겨찾기 REST API
 *
 * Base URL: /api/favorites
 *
 * ┌────────────────────────────────────────────────┐
 * │  POST   /api/favorites/{storeId}/toggle        │  토글 (추가 / 해제)
 * │  GET    /api/favorites/{storeId}/status        │  현재 즐겨찾기 여부
 * └────────────────────────────────────────────────┘
 *
 * 인증: Spring Security — @AuthenticationPrincipal 로 로그인 회원 식별
 * memberId는 UserDetails.getUsername() 에 저장된 ID(Long)를 파싱해서 사용
 * → 프로젝트의 CustomUserDetails 구조에 따라 getMemberId() 로 교체 가능
 */
import com.deushu.store.dto.FavoriteStoreDto;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    /**
     * 마이페이지 — 내 즐겨찾기 가게 목록
     * GET /api/favorites/my
     */
    @GetMapping("/my")
    public ResponseEntity<?> myFavorites(HttpSession session) {
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "ログインが必要です。"));
        }
        return ResponseEntity.ok(favoriteService.getFavoriteStores(memberId));
    }

    /**
     * 즐겨찾기 토글
     * POST /api/favorites/{storeId}/toggle
     *
     * Response:
     * {
     *   "favorited": true,   // true = 추가됨, false = 해제됨
     *   "storeId": 1
     * }
     */
    @PostMapping("/{storeId}/toggle")
    public ResponseEntity<Map<String, Object>> toggle(
            @PathVariable("storeId") Long storeId,
            HttpSession session) {

        // ── 비로그인 처리 ──
        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "ログインが必要です。"));
        }

        boolean favorited = favoriteService.toggle(memberId, storeId);

        return ResponseEntity.ok(Map.of(
                "favorited", favorited,
                "storeId",   storeId
        ));
    }

    /**
     * 즐겨찾기 여부 조회
     * GET /api/favorites/{storeId}/status
     *
     * Response:
     * {
     *   "favorited": true,
     *   "storeId": 1
     * }
     */
    @GetMapping("/{storeId}/status")
    public ResponseEntity<Map<String, Object>> status(
            @PathVariable("storeId") Long storeId,
            HttpSession session) {

        Long memberId = (Long) session.getAttribute("memberId");
        if (memberId == null) {
            return ResponseEntity.ok(Map.of("favorited", false, "storeId", storeId));
        }

        boolean isFav = favoriteService.isFavorited(memberId, storeId);

        return ResponseEntity.ok(Map.of(
                "favorited", isFav,
                "storeId",   storeId
        ));
    }

}