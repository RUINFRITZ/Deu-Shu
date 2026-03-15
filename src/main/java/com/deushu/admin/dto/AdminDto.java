package com.deushu.admin.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 관리자 기능 전용 DTO 모음
 * 모든 Admin DTO를 inner static class로 통합
 */
public class AdminDto {

    // ═══════════════════════════════════════
    // 응답 DTO
    // ═══════════════════════════════════════

    /** 대시보드 통계 — AdminMapper.findTodayStats */
    @Data
    public static class Dashboard {
        private int orderCount;
        private long totalSales;
        private int memberCount;
        private int activeStoreCount;
        private int totalStoreCount;
        private int yesterdayOrderCount;
        private long yesterdaySales;
        private int newMemberThisWeek;
    }

    /** 일별 매출 — AdminMapper.findWeeklySales */
    @Data
    public static class DailySales {
        private LocalDate saleDate;
        private int orderCount;
        private long totalSales;
    }

    /** 카테고리별 주문 통계 — AdminMapper.findCategoryOrderStats */
    @Data
    public static class CategoryStat {
        private String category;   // BAKERY / SUSHI / LUNCHBOX / CAFE / SIDEDISH
        private int orderCount;
        private long totalSales;
    }

    /** 회원 목록 — AdminMapper.findAllMembers */
    @Data
    public static class MemberRow {
        private Long id;
        private String email;
        private String lastName;
        private String firstName;
        private String lastNameKana;
        private String firstNameKana;
        private String phone;
        private String role;           // ROLE_USER / ROLE_OWNER / ROLE_ADMIN
        private int esgPoint;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;
    }

    /** 가게 목록 — AdminMapper.findAllStores (오너 JOIN) */
    @Data
    public static class StoreRow {
        private Long id;
        private String name;
        private String category;
        private String address;
        private String businessNumber;
        private LocalTime openTime;
        private LocalTime closeTime;
        private String thumbnailUrl;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
        // 오너 정보 (JOIN)
        private Long ownerId;
        private String ownerEmail;
        private String ownerName;      // CONCAT(last_name,' ',first_name)
    }

    /** 주문 목록 — AdminMapper.findAllOrders (회원 + 가게 JOIN) */
    @Data
    public static class OrderRow {
        private Long id;
        private String orderStatus;    // PAYMENT_PENDING / PAYMENT_COMPLETED / CANCELLED
        private int totalPrice;
        private String pickupCode;
        private LocalDateTime createdAt;
        // 회원 정보 (JOIN)
        private String memberName;
        private String email;
        // 가게 정보 (JOIN)
        private String storeName;
        private int itemCount;
    }

    /** 리뷰 목록 — AdminMapper.findAllReviews (회원 + 가게 JOIN) */
    @Data
    public static class ReviewRow {
        private Long id;
        private String title;
        private String content;
        private int rating;
        private String photoUrl;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
        // 회원 정보 (JOIN)
        private String memberName;
        private String email;
        // 가게 정보 (JOIN)
        private String storeName;
    }

    // ═══════════════════════════════════════
    // 요청 DTO
    // ═══════════════════════════════════════

    /** 역할 변경 요청 — PATCH /api/admin/members/{id}/role */
    @Data
    public static class RoleUpdateRequest {
        private String role; // ROLE_USER / ROLE_OWNER / ROLE_ADMIN
    }

    /** ESG 포인트 조정 요청 — PATCH /api/admin/members/{id}/esg-point */
    @Data
    public static class EsgPointRequest {
        private int esgPoint;
    }

    /** 주문 상태 변경 요청 — PATCH /api/admin/orders/{id}/status */
    @Data
    public static class OrderStatusRequest {
        private String status; // PAYMENT_PENDING / PAYMENT_COMPLETED / CANCELLED
    }
}