package com.deushu.store.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.deushu.common.util.Bbox;
import com.deushu.store.dto.NearbyStoreResponse;
import com.deushu.store.dto.StoreDetailDto;
import com.deushu.store.dto.StoreDetailResponse;
import com.deushu.store.dto.StoreFilterRequest;
import com.deushu.store.dto.StoreMapDto;
import com.deushu.store.service.StoreService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FR-M01 / FR-M03 / FR-M04 REST API 컨트롤러
 * 모든 엔드포인트: 비로그인 포함 전체 공개
 */
@Slf4j
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreApiController {

    private final StoreService storeService;
    
    @GetMapping("/detail/{storeId}")
    public StoreDetailResponse getStoreDetail(@PathVariable("storeId") Long storeId) {
        return storeService.getStoreDetail(storeId);
    }

    // ──────────────────────────────────────────────────────────────
    // FR-M01 + FR-M02  마감 할인 가게 핀 리스트 조회
    // GET /api/stores/pins?centerLat=&centerLng=&radius=
    // ──────────────────────────────────────────────────────────────

    /**
     * 지도 초기 로딩 시 호출.
     * [요구사항 1] index.html nav 버튼 → /list?radius=2 진입 시
     *             radius=2.0 이 기본값으로 전달되어 2km 내 가게만 핀 찍힘.
     *
     * @param centerLat 지도 중심 위도 (선택)
     * @param centerLng 지도 중심 경도 (선택)
     * @param radius    조회 반경 km  (선택, null → 전체)
     */
    @GetMapping("/pins")
    public ResponseEntity<List<StoreMapDto>> getStorePins(
            @RequestParam(name="centerLat", required = false) Double centerLat,
            @RequestParam(name="centerLng", required = false) Double centerLng,
            @RequestParam(name="radius",    required = false) Double radius) {

        // 좌표 유효성 기본 검사
        if ((centerLat != null && centerLng == null) ||
            (centerLat == null && centerLng != null)) {
            return ResponseEntity.badRequest().build();
        }

        List<StoreMapDto> pins =
            storeService.getStorePins(LocalDate.now(), centerLat, centerLng, radius);

        return ResponseEntity.ok(pins);
    }

    // ──────────────────────────────────────────────────────────────
    // [요구사항 4] 지도 이동 시 현재 뷰포트(Bbox) 내 가게 핀 조회
    // GET /api/stores/pins/bbox?minLat=&maxLat=&minLng=&maxLng=
    // ──────────────────────────────────────────────────────────────

    /**
     * [요구사항 4]
     * 지도 moveend 이벤트 시 현재 보이는 영역 내 가게 핀 실시간 조회.
     * 캐시 미적용 - 지도 이동마다 실시간 DB 조회.
     *
     * JS 호출 예시:
     *   const bounds = map.getBounds();
     *   /api/stores/pins/bbox?minLat=SW.lat&maxLat=NE.lat&minLng=SW.lng&maxLng=NE.lng
     *
     * @param minLat 남서쪽 위도 (SW)
     * @param maxLat 북동쪽 위도 (NE)
     * @param minLng 남서쪽 경도 (SW)
     * @param maxLng 북동쪽 경도 (NE)
     */
    @GetMapping("/pins/bbox")
    public ResponseEntity<List<StoreMapDto>> getStorePinsByBbox(
            @RequestParam(name="minLat") Double minLat,
            @RequestParam(name="maxLat") Double maxLat,
            @RequestParam(name="minLng") Double minLng,
            @RequestParam(name="maxLng") Double maxLng) {

        Bbox bbox = new Bbox(minLat, maxLat, minLng, maxLng);
        if (!bbox.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        List<StoreMapDto> pins = storeService.getStorePinsByBbox(bbox);
        return ResponseEntity.ok(pins);
    }

    // ──────────────────────────────────────────────────────────────
    // FR-M03  가게 상세 정보 조회
    // GET /api/stores/{storeId}
    // ──────────────────────────────────────────────────────────────

    /**
     * 지도 핀 클릭 시 사이드 패널 / 팝업 데이터 요청.
     * 로그인 사용자이면 즐겨찾기 여부도 함께 반환.
     *
     * @param storeId     stores.id
     * @param userDetails Spring Security Principal (비로그인 시 null)
     */
    @GetMapping("/{storeId}")
    public ResponseEntity<StoreDetailDto> getStoreDetail(
            @PathVariable("storeId") Long storeId,  // [버그수정] "id" → "storeId"로 통일
            @AuthenticationPrincipal UserDetails userDetails) {

        Long memberId = extractMemberId(userDetails);
        StoreDetailDto detail = storeService.getStoreDetail(storeId, memberId);
        return ResponseEntity.ok(detail);
    }

    // ──────────────────────────────────────────────────────────────
    // FR-M04  필터 기반 가게 검색
    // GET /api/stores/filter?category=BAKERY&minDiscountRate=30&...
    // ──────────────────────────────────────────────────────────────

    /**
     * 사용자 필터 적용 시 지도 핀 업데이트용.
     * 캐시 미적용 — 사용자별 실시간 조회.
     *
     * @param filter 카테고리, 할인율, 가격대, 마감시간, 위치 반경 등
     */
    @GetMapping("/filter")
    public ResponseEntity<List<StoreMapDto>> filterStores(
            @ModelAttribute StoreFilterRequest filter) {

        List<StoreMapDto> result = storeService.filterStores(filter);
        return ResponseEntity.ok(result);
    }

    // ──────────────────────────────────────────────────────────────
    // [요구사항 3] 무한 스크롤 페이징 (radius 파라미터 추가)
    // GET /api/stores?page=1&size=10&centerLat=&centerLng=&radius=2
    // ──────────────────────────────────────────────────────────────

    /**
     * [요구사항 3]
     * 우측 리스트 패널 무한 스크롤용.
     * radius 파라미터가 있으면 해당 반경 내 가게만 리스트에 표시됨
     * → 지도 핀 범위와 리스트 범위가 일치.
     *
     * @param page      페이지 번호 (0-based, 기본 0)
     * @param size      페이지 크기 (기본 10)
     * @param centerLat 위도 (거리 계산 및 반경 필터용, 선택)
     * @param centerLng 경도 (거리 계산 및 반경 필터용, 선택)
     * @param radius    조회 반경 km (선택, null → 전체)
     */
    @GetMapping
    public ResponseEntity<NearbyStoreResponse> getStoresByPage(
            @RequestParam(name="page",      defaultValue = "0")  int    page,
            @RequestParam(name="size",      defaultValue = "10") int    size,
            @RequestParam(name="centerLat", required = false)    Double centerLat,
            @RequestParam(name="centerLng", required = false)    Double centerLng,
            @RequestParam(name="radius",    required = false)    Double radius) {

        NearbyStoreResponse response =
            storeService.getStoresByPage(page, size, centerLat, centerLng, radius);
        return ResponseEntity.ok(response);
    }

    // ──────────────────────────────────────────────────────────────
    // [요구사항 2] 관리자용 캐시 수동 무효화
    // POST /api/stores/admin/cache/evict
    // ──────────────────────────────────────────────────────────────

    /**
     * [요구사항 2]
     * 새 가게 등록 후 캐시 수동 무효화용 엔드포인트.
     * 가게 등록/삭제 등 변경 이벤트 시 호출.
     *
     * 권장 방법:
     *  - 가게 등록 서비스(StoreAdminService)에
     *    @CacheEvict(value = "todayStorePins", allEntries = true) 를 직접 붙이거나
     *  - 이 엔드포인트를 관리자 화면에서 수동 호출
     *
     * 보안 주의: SecurityConfig에서 ADMIN 권한만 허용하도록 추가 필요.
     *   .requestMatchers("/api/stores/admin/**").hasRole("ADMIN")
     */
    @PostMapping("/admin/cache/evict")
    public ResponseEntity<Map<String, String>> evictCache() {
        storeService.evictStorePinsCache();
        log.info("[요구사항2] 관리자 캐시 수동 무효화 완료");
        return ResponseEntity.ok(Map.of("result", "캐시 무효화 완료"));
    }

    // ──────────────────────────────────────────────────────────────
    // Favorites (즐겨찾기) 토글
    // POST /api/stores/{storeId}/favorite/toggle
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/{storeId}/favorite/toggle")
    public ResponseEntity<Map<String, Boolean>> toggleFavorite(
            @PathVariable("storeId") Long storeId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long memberId = extractMemberId(userDetails);
        if (memberId == null) {
            return ResponseEntity.status(401).build();
        }

        boolean favorited = storeService.toggleFavorite(storeId, memberId);
        return ResponseEntity.ok(Map.of("favorited", favorited));
    }

    // ──────────────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────────────

    /**
     * Spring Security UserDetails에서 회원 ID를 추출한다.
     * CustomUserDetails 구현체에 맞게 수정 필요.
     * 비로그인 또는 GUEST이면 null 반환.
     */
    private Long extractMemberId(UserDetails userDetails) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long) return (Long) principal;
        return null;
    }
}