package com.deushu.review.mapper;

import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse.ReviewItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
	
	// cursor 기반 페이징 조회
    List<ReviewItem> findByStoreId(
            @Param("storeId") long storeId,
            @Param("cursor")  Long cursor,   // null이면 첫 페이지
            @Param("size")    int  size
    );

    int existsByOrderId(@Param("orderId") long orderId);

    int insert(ReviewCreateRequest req);

    // 리뷰 삭제 (논리 삭제 — 본인만 가능)
    int deleteById(@Param("reviewId") long reviewId,
                   @Param("memberId") long memberId);
}