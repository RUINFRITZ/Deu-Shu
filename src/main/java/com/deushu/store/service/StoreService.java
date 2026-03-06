package com.deushu.store.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.deushu.common.util.Bbox;
import com.deushu.store.dto.NearbyStoreResponse;
import com.deushu.store.dto.StoreDetailDto;
import com.deushu.store.dto.StoreDetailResponse;
import com.deushu.store.dto.StoreFilterRequest;
import com.deushu.store.dto.StoreMapDto;
import com.deushu.store.mapper.StoreMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FR-M01 ~ FR-M05 비즈니스 로직
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreMapper storeMapper;
    
    public StoreDetailResponse getStoreDetail(Long storeId) {
        StoreDetailResponse.StoreInfo store = storeMapper.findStoreInfo(storeId);
        if (store == null) {
            throw new IllegalArgumentException("STORE_NOT_FOUND: " + storeId);
        }

        List<String> images = storeMapper.findStoreImages(storeId);
        StoreDetailResponse.RatingInfo rating = storeMapper.findRatingInfo(storeId);
        List<StoreDetailResponse.ItemInfo> items = storeMapper.findSellingItems(storeId);

        // 리뷰 0개면 null 방지
        if (rating == null) {
            rating = new StoreDetailResponse.RatingInfo();
            rating.setAvg(0.0);
            rating.setCount(0);
        } else {
            if (rating.getAvg() == null) rating.setAvg(0.0);
            if (rating.getCount() == null) rating.setCount(0);
        }

        StoreDetailResponse res = new StoreDetailResponse();
        res.setStore(store);
        res.setImages(images);
        res.setRating(rating);
        res.setItems(items);
        return res;
    }
    // ══════════════════════════════════════════════════════════════
    // FR-M01 + FR-M02  마감 할인 가게 핀 리스트 (캐시 적용)
    // ══════════════════════════════════════════════════════════════

    /**
     * FR-M01 + FR-M02
     * 오늘 기준 활성 마감 할인 가게 핀 리스트를 조회한다.
     * Caffeine 캐시(todayStorePins)에 1시간 TTL로 저장.
     *
     * 캐시 Key = "날짜:위도:경도:반경"
     * → 같은 날, 같은 지역/반경 조회는 캐시 HIT
     *
     * [요구사항 1] nav 버튼에서 radius=2로 진입 시 캐시 키에 반경이 포함되므로
     *             2km 기준 결과가 별도로 캐싱됨. 혼용 시 충돌 없음.
     */
    @Cacheable(
        value = "todayStorePins",
        key =
            "(#targetDate != null ? #targetDate.toString() : 'NONE')"
            + " + ':' + "
            + "(#centerLat  != null ? #centerLat  : 'NONE')"
            + " + ':' + "
            + "(#centerLng  != null ? #centerLng  : 'NONE')"
            + " + ':' + "
            + "(#radius     != null ? #radius : 1000)"
    )
    public List<StoreMapDto> getStorePins(LocalDate targetDate,
                                          Double centerLat,
                                          Double centerLng,
                                          Double radius) {
        log.debug("[FR-M01] Cache MISS → DB 조회 | date={} lat={} lng={} radius={}",
                  targetDate, centerLat, centerLng, radius);
        return storeMapper.findStorePins(centerLat, centerLng, radius);
    }

    // ══════════════════════════════════════════════════════════════
    // [요구사항 4] 지도 이동 시 현재 뷰포트 내 가게 핀 조회 (캐시 미적용)
    // ══════════════════════════════════════════════════════════════

    /**
     * [요구사항 4]
     * 지도 moveend 이벤트 시 현재 보이는 영역(Bbox)의 가게 핀을 실시간 조회.
     * 지도 이동마다 호출되므로 캐시 미적용.
     * Bbox.isValid() 실패 시 빈 리스트 반환.
     *
     * @param bbox Leaflet map.getBounds() 기반 SW/NE 좌표
     */
    public List<StoreMapDto> getStorePinsByBbox(Bbox bbox) {
        if (bbox == null || !bbox.isValid()) {
            log.warn("[요구사항4] 유효하지 않은 Bbox → 빈 리스트 반환");
            return List.of();
        }
        log.debug("[요구사항4] Bbox 핀 조회 | lat=[{},{}] lng=[{},{}]",
                  bbox.getMinLat(), bbox.getMaxLat(), bbox.getMinLng(), bbox.getMaxLng());
        return storeMapper.findStorePinsByBbox(bbox);
    }

    // ══════════════════════════════════════════════════════════════
    // FR-M03  가게 상세 정보 조회
    // ══════════════════════════════════════════════════════════════

    /**
     * FR-M03
     * 지도 팝업 / 사이드 패널용 가게 상세 정보 조회.
     * stores + items + store_images + favorites(즐겨찾기 여부) 통합 반환.
     *
     * @param storeId  조회할 가게 ID
     * @param memberId 로그인 회원 ID (비로그인 시 null)
     * @throws ResponseStatusException 404 — 가게가 없거나 삭제된 경우
     */
    public StoreDetailDto getStoreDetail(Long storeId, Long memberId) {

        // 1. 가게 기본 정보 + 즐겨찾기 여부 조회
        StoreDetailDto detail = storeMapper.findStoreDetail(storeId, memberId);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "가게를 찾을 수 없습니다. storeId=" + storeId);
        }

        // 2. 활성 상품 목록 조회 (items)
        List<StoreDetailDto.ItemSummaryDto> items =
            storeMapper.findActiveItemsByStore(storeId);

        // 3. 가게 이미지 목록 조회 (store_images)
        List<String> imageUrls = storeMapper.findStoreImageUrls(storeId);

        // 4. 복합 DTO 재구성 (Builder로 items / imageUrls 추가)
        return StoreDetailDto.builder()
            .id(detail.getId())
            .name(detail.getName())
            .category(detail.getCategory())
            .address(detail.getAddress())
            .lat(detail.getLat())
            .lng(detail.getLng())
            .openTime(detail.getOpenTime())
            .closeTime(detail.getCloseTime())
            .thumbnailUrl(detail.getThumbnailUrl())
            .avgRating(detail.getAvgRating())
            .reviewCount(detail.getReviewCount())
            .favorited(detail.isFavorited())
            .items(items)
            .imageUrls(imageUrls)
            .build();
    }

    // ══════════════════════════════════════════════════════════════
    // FR-M04  필터 기반 가게 검색
    // ══════════════════════════════════════════════════════════════

    /**
     * FR-M04
     * 카테고리, 할인율, 가격대, 마감시간, 거리 등의 필터를 적용해
     * 조건에 맞는 가게 핀 목록을 반환한다.
     * 사용자별 실시간 필터이므로 캐시 미적용.
     *
     * @param filter 필터 파라미터 DTO
     */
    public List<StoreMapDto> filterStores(StoreFilterRequest filter) {
        log.debug("[FR-M04] 필터 검색 | category={} minRate={} maxPrice={} expireWithin={}",
                  filter.getCategory(), filter.getMinDiscountRate(),
                  filter.getMaxDiscountPrice(), filter.getExpireWithinMinutes());
        return storeMapper.findStorePinsFiltered(filter);
    }

    // ══════════════════════════════════════════════════════════════
    // FR-M05  캐시 무효화
    // ══════════════════════════════════════════════════════════════

    /**
     * FR-M05 + [요구사항 2]
     * todayStorePins 캐시 전체 무효화.
     *
     * 호출 시점:
     *  - 주문 완료 / 재고 차감 시 (PaymentService에서 호출)
     *  - [요구사항 2] 신규 가게 등록 시 (StoreAdminService.registerStore 완료 후 호출)
     *    → 새 가게가 등록되어도 캐시가 남아 있으면 핀이 안 찍히는 문제를 해결.
     *    → StoreAdminService.registerStore() 메서드에
     *       @CacheEvict(value = "todayStorePins", allEntries = true) 를 직접 붙이거나,
     *       해당 메서드 내부에서 이 evictStorePinsCache()를 호출하면 된다.
     *
     * @CacheEvict(allEntries = true) → 캐시 전체 삭제
     */
    @CacheEvict(value = "todayStorePins", allEntries = true)
    public void evictStorePinsCache() {
        log.info("[FR-M05/요구사항2] todayStorePins 캐시 전체 무효화 완료");
    }

    // ══════════════════════════════════════════════════════════════
    // [요구사항 3] 무한 스크롤 페이징 (radius 파라미터 추가)
    // ══════════════════════════════════════════════════════════════

    /**
     * [요구사항 3]
     * 무한 스크롤용 페이징 가게 목록 조회.
     * radius가 non-null일 때 centerLat/Lng 기준 반경 내 가게만 반환
     * → 지도 핀 범위와 리스트 범위가 일치한다.
     *
     * @param page      페이지 번호 (0-based)
     * @param size      페이지 크기
     * @param centerLat 위도 (거리 계산 및 반경 필터용, nullable)
     * @param centerLng 경도 (거리 계산 및 반경 필터용, nullable)
     * @param radius    조회 반경 km (nullable → 전체)
     */
    public NearbyStoreResponse getStoresByPage(int page, int size,
                                                Double centerLat, Double centerLng,
                                                Double radius) {
        int offset = page * size;
        List<StoreMapDto> content =
            storeMapper.findStoresByPage(centerLat, centerLng, radius, offset, size);
        long total = storeMapper.countActiveStores(centerLat, centerLng, radius);

        return NearbyStoreResponse.of(content, page, size, total);
    }

    /**
     * 즐겨찾기 토글
     * - 이미 즐겨찾기 되어 있으면 삭제 후 false 반환
     * - 없으면 INSERT 후 true 반환
     */
    @Transactional
    public boolean toggleFavorite(Long storeId, Long memberId) {
        if (memberId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        boolean exists = storeMapper.existsFavorite(memberId, storeId);
        if (exists) {
            storeMapper.deleteFavorite(memberId, storeId);
            return false; // 즐겨찾기 해제
        } else {
            storeMapper.insertFavorite(memberId, storeId);
            return true;  // 즐겨찾기 등록
        }
    }
}