package com.deushu.admin.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.deushu.admin.dto.AdminDto;

@Mapper
public interface AdminRepository {

    // ── 대시보드 ──────────────────────────────────────────
    AdminDto.Dashboard findTodayStats();
    List<AdminDto.DailySales> findWeeklySales();
    List<AdminDto.CategoryStat> findCategoryOrderStats();

    // ── 회원 관리 ──────────────────────────────────────────
    List<AdminDto.MemberRow> findAllMembers(
        @Param("keyword") String keyword,
        @Param("role") String role,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );
    int countAllMembers(@Param("keyword") String keyword, @Param("role") String role);
    void updateMemberRole(@Param("memberId") Long memberId, @Param("role") String role);
    void suspendMember(@Param("memberId") Long memberId);   // 정지
    void restoreMember(@Param("memberId") Long memberId);   // 정지 해제
    void withdrawMember(@Param("memberId") Long memberId);  // 탈퇴

    // ── 가게 관리 ──────────────────────────────────────────
    List<AdminDto.StoreRow> findAllStores(
        @Param("keyword") String keyword,
        @Param("category") String category,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );
    int countAllStores(@Param("keyword") String keyword, @Param("category") String category);
    void deactivateStore(@Param("storeId") Long storeId, @Param("stopReason") String stopReason);
    void restoreStore(@Param("storeId") Long storeId);

    // ── 주문 관리 ──────────────────────────────────────────
    List<AdminDto.OrderRow> findAllOrders(
        @Param("keyword") String keyword,
        @Param("orderStatus") String orderStatus,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );
    int countAllOrders(@Param("keyword") String keyword, @Param("orderStatus") String orderStatus);
    void updateOrderStatus(@Param("orderId") Long orderId, @Param("orderStatus") String orderStatus);

    // ── 리뷰 관리 ──────────────────────────────────────────
    List<AdminDto.ReviewRow> findAllReviews(
        @Param("keyword") String keyword,
        @Param("maxRating") Double maxRating,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );
    int countAllReviews(@Param("keyword") String keyword, @Param("maxRating") Double maxRating);
    void deleteReview(@Param("reviewId") Long reviewId);

    // ── 클레임 관리 ────────────────────────────────────────
    List<AdminDto.ComplaintRow> findAllComplaints(
        @Param("status") String status,
        @Param("complaintType") String complaintType,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );
    int countAllComplaints(@Param("status") String status, @Param("complaintType") String complaintType);
    void updateComplaintReply(
        @Param("complaintId") Long complaintId,
        @Param("adminReply") String adminReply,
        @Param("status") String status
    );
}