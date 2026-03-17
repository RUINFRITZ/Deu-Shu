package com.deushu.admin.dto;

import lombok.Data;
import java.time.LocalDateTime;

public class AdminDto {

    // 대시보드 통계
    @Data
    public static class Dashboard {
        private int orderCount;
        private long totalSales;
        private int memberCount;
        private int activeStoreCount;
    }

    // 일별 매출 (바 차트용)
    @Data
    public static class DailySales {
        private String saleDate;
        private int orderCount;
        private long totalSales;
    }

    // 카테고리별 주문 통계 (도넛 차트용)
    @Data
    public static class CategoryStat {
        private String category;
        private int orderCount;
    }

    // 회원 목록
    @Data
    public static class MemberRow {
        private Long id;
        private String email;
        private String lastName;
        private String firstName;
        private String lastNameKana;
        private String firstNameKana;
        private String phone;
        private String role;
        private int esgPoint;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
    }

    // 가게 목록 (오너 정보 JOIN)
    @Data
    public static class StoreRow {
        private Long id;
        private String name;
        private String category;
        private String address;
        private String openTime;
        private String closeTime;
        private String ownerName;
        private String ownerEmail;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
    }

    // 주문 목록
    @Data
    public static class OrderRow {
        private Long id;
        private String memberName;
        private String email;
        private String storeName;
        private int totalPrice;
        private int itemCount;
        private String pickupCode;
        private String orderStatus;
        private LocalDateTime createdAt;
    }

    // 리뷰 목록
    @Data
    public static class ReviewRow {
        private Long id;
        private String storeName;
        private String memberName;
        private String email;
        private String title;
        private String content;
        private double rating;
        private LocalDateTime createdAt;
    }

    // 클레임 목록
    @Data
    public static class ComplaintRow {
        private Long id;
        private String reporterName;
        private String reporterEmail;
        private String targetStoreName;
        private String complaintType;
        private String title;
        private String content;
        private String status;
        private String adminReply;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    // 역할 변경 요청
    @Data
    public static class RoleUpdateRequest {
        private Long memberId;
        private String role;
    }

    // ESG 포인트 조정 요청
    @Data
    public static class EsgPointRequest {
        private Long memberId;
        private int esgPoint;
    }

    // 주문 상태 변경 요청
    @Data
    public static class OrderStatusRequest {
        private Long orderId;
        private String orderStatus;
    }

    // 클레임 답변 요청
    @Data
    public static class ComplaintReplyRequest {
        private Long complaintId;
        private String adminReply;
        private String status;
    }
}