package com.deushu.review.service;

import com.deushu.order.domain.OrderEntity;
import com.deushu.order.mapper.OrderMapper;
import com.deushu.review.dto.ReviewCreateRequest;
import com.deushu.review.dto.ReviewListResponse;
import com.deushu.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final OrderMapper  orderMapper;


    public ReviewListResponse listByStore(long storeId) {
        ReviewListResponse res = new ReviewListResponse();
        res.setItems(reviewMapper.findByStoreId(storeId));
        res.setNextCursor(null);
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
        if (!hasPurchased) {
            throw new IllegalStateException("해당 가게에서 결제 완료된 주문이 없습니다.");
        }
        
     // 리뷰 작성 기한 3일 체크
        if (req.getOrderId() != null) {
            OrderEntity order = orderMapper.findOrderById(req.getOrderId());
            if (order != null && order.getCreatedAt() != null) {
                LocalDateTime deadline = order.getCreatedAt().plusDays(3);
                if (LocalDateTime.now().isAfter(deadline)) {
                    throw new IllegalStateException("리뷰 작성 기한(3일)이 지났습니다.");
                }
            }
        }
        
        // orderId가 있는 경우에만 중복 체크 (결제 연동 후 확장 가능)
        if (req.getOrderId() != null && reviewMapper.existsByOrderId(req.getOrderId()) > 0) {
            throw new IllegalStateException("이미 해당 주문에 리뷰가 존재합니다.");
        }

        reviewMapper.insert(req);
    }
}