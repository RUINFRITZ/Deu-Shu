package com.deushu.review.mapper;

import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse.ReviewItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {
    List<ReviewItem> findByStoreId(@Param("storeId") long storeId);

    int existsByOrderId(@Param("orderId") long orderId);

    int insert(ReviewCreateRequest req);

    // 리뷰 삭제 (논리 삭제 — 본인만 가능)
    int deleteById(@Param("reviewId") long reviewId,
                   @Param("memberId") long memberId);
}