package com.deushu.review.service;

import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse;
import com.deushu.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewListResponse listByStore(long storeId) {
        ReviewListResponse res = new ReviewListResponse();
        res.setItems(reviewMapper.findByStoreId(storeId));
        res.setNextCursor(null);
        return res;
    }

    public void create(ReviewCreateRequest req) {
        if (req.getStoreId() == null) throw new IllegalArgumentException("storeId 필수");
        if (req.getOrderId() == null) throw new IllegalArgumentException("orderId 필수");
        if (req.getMemberId() == null) throw new IllegalArgumentException("memberId 필수(임시)");

        if (!StringUtils.hasText(req.getTitle())) throw new IllegalArgumentException("title 필수");
        if (!StringUtils.hasText(req.getContent())) throw new IllegalArgumentException("content 필수");
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5)
            throw new IllegalArgumentException("rating 1~5");

        // ✅ 주문당 1리뷰(UNIQUE order_id)라서 중복 방지(친절하게 400 처리)
        if (reviewMapper.existsByOrderId(req.getOrderId()) > 0) {
            throw new IllegalStateException("이미 해당 주문에 리뷰가 존재합니다.");
        }

        reviewMapper.insert(req);
    }
}