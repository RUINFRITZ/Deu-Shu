package com.deushu.store.mapper;

import com.deushu.common.util.Bbox;
import com.deushu.store.dto.StoreDetailDto;
import com.deushu.store.dto.StoreFilterRequest;
import com.deushu.store.dto.StoreMapDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis 스토어 매퍼
 * 모든 SQL은 StoreMapper.xml 에서 관리
 */
@Mapper
public interface StoreMapper {

    /**
     * FR-M01 / FR-M02
     * 오늘 기준 활성 마감 할인 가게 핀 전체 조회
     */
    List<StoreMapDto> findStorePins(@Param("centerLat") Double centerLat,
                                    @Param("centerLng") Double centerLng,
                                    @Param("radius")    Double radius);

    /**
     * [요구사항 4] 지도 이동 시 현재 뷰포트(Bbox) 내 가게 핀 조회
     * Leaflet map.getBounds() SW/NE 좌표를 받아 해당 영역 가게만 반환.
     * 캐시 미적용 - 지도 이동마다 실시간 조회.
     */
    List<StoreMapDto> findStorePinsByBbox(@Param("bbox") Bbox bbox);

    /**
     * FR-M04 필터 조건 기반 가게 핀 조회 (동적 SQL)
     */
    List<StoreMapDto> findStorePinsFiltered(@Param("f") StoreFilterRequest filter);

    /**
     * FR-M03 가게 상세 정보 (가게 기본 + 활성 상품 목록)
     */
    StoreDetailDto findStoreDetail(@Param("storeId")  Long storeId,
                                   @Param("memberId") Long memberId);

    /** FR-M03 보조 — 가게 이미지 목록 조회 */
    List<String> findStoreImageUrls(@Param("storeId") Long storeId);

    /** FR-M03 보조 — 가게의 활성 상품 목록 조회 */
    List<StoreDetailDto.ItemSummaryDto> findActiveItemsByStore(@Param("storeId") Long storeId);

    /**
     * [요구사항 3] 무한 스크롤 페이징용 가게 목록 조회 (radius 필터 추가)
     * radius가 null이 아닐 때 centerLat/Lng 기준 반경 내 가게만 조회.
     */
    List<StoreMapDto> findStoresByPage(@Param("centerLat") Double centerLat,
                                       @Param("centerLng") Double centerLng,
                                       @Param("radius")    Double radius,
                                       @Param("offset")    int offset,
                                       @Param("limit")     int limit);

    /**
     * [요구사항 3] 무한 스크롤용 전체 건수 (radius 필터 포함)
     * radius null → 전체 건수, non-null → 반경 내 건수.
     */
    long countActiveStores(@Param("centerLat") Double centerLat,
                           @Param("centerLng") Double centerLng,
                           @Param("radius")    Double radius);

    // ══════════════════════════════════════════════════════════════
    // Favorites (즐겨찾기)
    // ══════════════════════════════════════════════════════════════

    boolean existsFavorite(@Param("memberId") Long memberId,
                           @Param("storeId")  Long storeId);

    int insertFavorite(@Param("memberId") Long memberId,
                       @Param("storeId")  Long storeId);

    int deleteFavorite(@Param("memberId") Long memberId,
                       @Param("storeId")  Long storeId);
}