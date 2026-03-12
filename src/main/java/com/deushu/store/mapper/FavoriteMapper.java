package com.deushu.store.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * FavoriteMapper — favorites 테이블 CRUD
 *
 * SQL은 FavoriteMapper.xml 에 작성
 */
@Mapper
public interface FavoriteMapper {

    /**
     * 즐겨찾기 추가
     * 이미 존재하면 INSERT IGNORE로 중복 무시
     */
    int insert(@Param("memberId") Long memberId,
               @Param("storeId")  Long storeId);

    /**
     * 즐겨찾기 삭제
     * @return 삭제된 행 수 (0 이면 원래 없던 것)
     */
    int delete(@Param("memberId") Long memberId,
               @Param("storeId")  Long storeId);

    /**
     * 즐겨찾기 여부 확인
     * @return true = 이미 즐겨찾기 상태
     */
    boolean exists(@Param("memberId") Long memberId,
                   @Param("storeId")  Long storeId);

    /**
     * 회원이 즐겨찾기한 storeId 목록
     */
    List<Long> findStoreIdsByMember(@Param("memberId") Long memberId);

    /**
     * 마이페이지용: 즐겨찾기 가게 상세 목록 (stores JOIN)
     */
    List<com.deushu.store.dto.FavoriteStoreDto> findFavoriteStoresByMember(@Param("memberId") Long memberId);

    /**
     * 특정 가게의 즐겨찾기 수 (인기도 표시용)
     */
    int countByStore(@Param("storeId") Long storeId);
}