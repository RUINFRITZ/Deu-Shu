package com.deushu.review.service;

import com.deushu.order.domain.OrderEntity;
import com.deushu.order.mapper.OrderMapper;
import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse;
import com.deushu.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper  orderMapper;
    
    private static final int PAGE_SIZE = 10;

    public ReviewListResponse listByStore(long storeId, Long cursor) {
        // size+1 개 조회해서 다음 페이지 존재 여부 판단
        List<ReviewListResponse.ReviewItem> items =
                reviewMapper.findByStoreId(storeId, cursor, PAGE_SIZE + 1);

        boolean hasNext = items.size() > PAGE_SIZE;
        if (hasNext) items = items.subList(0, PAGE_SIZE);
 
        Long nextCursor = hasNext ? items.get(items.size() - 1).getId() : null;
 
        ReviewListResponse res = new ReviewListResponse();
        res.setItems(items);
        res.setNextCursor(nextCursor);
        return res;
    }
        
    public void create(ReviewCreateRequest req) {
        if (req.getStoreId()  == null) throw new IllegalArgumentException("storeId 必須");
        if (req.getMemberId() == null) throw new IllegalArgumentException("memberId 必須");
        if (!StringUtils.hasText(req.getTitle()))   throw new IllegalArgumentException("タイトルを入力してください。");
        if (!StringUtils.hasText(req.getContent())) throw new IllegalArgumentException("内容を入力してください。");
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5)
            throw new IllegalArgumentException("評価は1〜5の間で入力してください。");

        // 결제 완료된 주문이 있는지 확인
        boolean hasPurchased = orderMapper.existsCompletedOrder(req.getMemberId(), req.getStoreId());
        if (!hasPurchased)
            throw new IllegalStateException("この店舗では、決済が完了した注文はありません。");

        // 리뷰 작성 기한 3일 체크
        if (req.getOrderId() != null) {
            OrderEntity order = orderMapper.findOrderById(req.getOrderId());
            if (order != null && order.getCreatedAt() != null) {
                if (LocalDateTime.now().isAfter(order.getCreatedAt().plusDays(3)))
                    throw new IllegalStateException("レビュー作成期限（3日）が過ぎました。");
            }
        }

        // 중복 리뷰 방지
        if (req.getOrderId() != null && reviewMapper.existsByOrderId(req.getOrderId()) > 0)
            throw new IllegalStateException("すでにその注文にはレビューが存在します。");

        reviewMapper.insert(req);
    }

    public void delete(long reviewId, long memberId) {
        int affected = reviewMapper.deleteById(reviewId, memberId);
        if (affected == 0)
            throw new IllegalStateException("削除できません。 (自分のレビューでないか、すでに削除されたレビューです。)");
    }
}