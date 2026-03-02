package com.deushu.review.mapper;

import com.deushu.review.dto.ReviewListResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ReviewMapper {

    // storeId의 리뷰를 최신순으로 가져오기 (cursor 있으면 id < cursor)
    List<ReviewListResponse.ReviewItem> findReviews(
            @Param("storeId") Long storeId,
            @Param("cursor") Long cursor,
            @Param("limit") int limit
    );
}