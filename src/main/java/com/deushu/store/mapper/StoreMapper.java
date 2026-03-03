package com.deushu.store.mapper;

import com.deushu.store.dto.StoreDetailResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StoreMapper {

    StoreDetailResponse.StoreInfo findStoreInfo(@Param("storeId") Long storeId);

    List<String> findStoreImages(@Param("storeId") Long storeId);

    StoreDetailResponse.RatingInfo findRatingInfo(@Param("storeId") Long storeId);

    List<StoreDetailResponse.ItemInfo> findSellingItems(@Param("storeId") Long storeId);
}