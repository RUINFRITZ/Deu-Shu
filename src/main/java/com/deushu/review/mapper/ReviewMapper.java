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
}