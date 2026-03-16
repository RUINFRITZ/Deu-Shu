package com.deushu.store.mapper;

import com.deushu.store.dto.ReviewForSummaryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * AI 리뷰 요약 전용 MyBatis 매퍼
 * 기존 StoreMapper / ReviewMapper 와 충돌 없이 독립적으로 운용
 */
@Mapper
public interface ReviewSummaryMapper {

    /**	
     * 특정 가게의 전체 리뷰를 AI 요약 처리용으로 조회
     * 삭제된 리뷰(deleted_at IS NOT NULL) 제외
     * 최신순 정렬
     *
     * @param storeId 가게 ID
     * @return 리뷰 목록 (title, content, rating)
     */
    List<ReviewForSummaryDto> findAllReviewsByStoreId(@Param("storeId") Long storeId);
}