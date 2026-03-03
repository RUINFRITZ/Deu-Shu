package com.deushu.store.service;

import com.deushu.store.dto.StoreDetailResponse;
import com.deushu.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
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
}