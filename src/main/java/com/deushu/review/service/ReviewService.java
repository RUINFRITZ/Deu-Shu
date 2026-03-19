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
        if (req.getStoreId()  == null) throw new IllegalArgumentException("storeId 필수");
        if (req.getMemberId() == null) throw new IllegalArgumentException("memberId 필수");
        if (!StringUtils.hasText(req.getTitle()))   throw new IllegalArgumentException("제목을 입력해주세요.");
        if (!StringUtils.hasText(req.getContent())) throw new IllegalArgumentException("내용을 입력해주세요.");
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5)
            throw new IllegalArgumentException("별점은 1~5 사이로 입력해주세요.");

        // 결제 완료된 주문이 있는지 확인
        boolean hasPurchased = orderMapper.existsCompletedOrder(req.getMemberId(), req.getStoreId());
        if (!hasPurchased)
            throw new IllegalStateException("해당 가게에서 결제 완료된 주문이 없습니다.");

        // 리뷰 작성 기한 3일 체크
        if (req.getOrderId() != null) {
            OrderEntity order = orderMapper.findOrderById(req.getOrderId());
            if (order != null && order.getCreatedAt() != null) {
                if (LocalDateTime.now().isAfter(order.getCreatedAt().plusDays(3)))
                    throw new IllegalStateException("리뷰 작성 기한(3일)이 지났습니다.");
            }
        }

        // 중복 리뷰 방지
        if (req.getOrderId() != null && reviewMapper.existsByOrderId(req.getOrderId()) > 0)
            throw new IllegalStateException("이미 해당 주문에 리뷰가 존재합니다.");

        reviewMapper.insert(req);
    }

    public void delete(long reviewId, long memberId) {
        int affected = reviewMapper.deleteById(reviewId, memberId);
        if (affected == 0)
            throw new IllegalStateException("삭제할 수 없습니다. (본인 리뷰가 아니거나 이미 삭제된 리뷰입니다.)");
    }
}