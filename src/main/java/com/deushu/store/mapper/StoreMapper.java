package com.deushu.store.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deushu.common.util.Bbox;
import com.deushu.store.domain.StoreEntity;
import com.deushu.store.dto.StoreDetailDto;
import com.deushu.store.dto.StoreDetailResponse;
import com.deushu.store.dto.StoreFilterRequest;
import com.deushu.store.dto.StoreImageDto;
import com.deushu.store.dto.StoreMapDto;

/**
 * MyBatis 스토어 매퍼
 * 모든 SQL은 StoreMapper.xml 에서 관리
 */
@Mapper
public interface StoreMapper {

	
    StoreDetailResponse.StoreInfo findStoreInfo(@Param("storeId") Long storeId);

    List<String> findStoreImages(@Param("storeId") Long storeId);

    StoreDetailResponse.RatingInfo findRatingInfo(@Param("storeId") Long storeId);

    List<StoreDetailResponse.ItemInfo> findSellingItems(@Param("storeId") Long storeId);
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

    List<StoreMapDto> findStorePinsByBboxFiltered(@Param("bbox") Bbox bbox,
            @Param("f") StoreFilterRequest filter);

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
    
    // ── stores 조회 ──────────────────────────────

    /** 오너 ID로 가게 조회 (논리삭제 제외) */
    StoreEntity findByOwnerId(Long ownerId);

    /** 가게 ID로 가게 조회 */
    StoreEntity findStoreById(Long storeId); // 기존 findById와 이름 충돌 방지

    // ── stores 등록 / 수정 ───────────────────────

    void upsertStore(StoreEntity store);

    /** 주소·위경도만 단독 업데이트 */
    void updateAddress(StoreEntity store);

    /** 가게 논리삭제 */
    void delete(Long storeId);

    // ── store_images 조회 ────────────────────────

    /** 가게의 모든 추가 사진 목록 조회 (sort_order 오름차순) */
    List<StoreImageDto> findImagesByStoreId(Long storeId);

    // ── store_images 등록 / 삭제 ─────────────────

    /** 가게 사진 추가 */
    void insertImage(StoreImageDto image);

    /** 가게 사진 삭제 */
    void deleteImage(@Param("imageId") Long imageId, @Param("storeId") Long storeId);

    // ── mock_business_registry 조회 ──────────────

    /** 사업자 번호로 사업자 정보 조회 (오너 회원가입 검증용) */
    StoreEntity findBusinessByNumber(String businessNumber);
    
    List<StoreMapDto> findStoresByPageFiltered(@Param("f") StoreFilterRequest filter,
            @Param("offset") int offset,
            @Param("limit") int limit);

    long countActiveStoresFiltered(@Param("f") StoreFilterRequest filter);
}