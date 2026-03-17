package com.deushu.admin.service;

import com.deushu.admin.dto.AdminDto;
import com.deushu.admin.mapper.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository AdminRepository;

    // ── 대시보드
    public AdminDto.Dashboard getTodayStats() {
        return AdminRepository.findTodayStats();
    }
    public List<AdminDto.DailySales> getWeeklySales() {
        return AdminRepository.findWeeklySales();
    }
    public List<AdminDto.CategoryStat> getCategoryStats() {
        return AdminRepository.findCategoryOrderStats();
    }

    // ── 회원
    public List<AdminDto.MemberRow> getMembers(String keyword, String role, int page, int size) {
        return AdminRepository.findAllMembers(keyword, role, page * size, size);
    }
    public int countMembers(String keyword, String role) {
        return AdminRepository.countAllMembers(keyword, role);
    }
    public void updateMemberRole(Long memberId, String role) {
        AdminRepository.updateMemberRole(memberId, role);
    }
    public void suspendMember(Long memberId) {
        AdminRepository.suspendMember(memberId);
    }
    public void restoreMember(Long memberId) {
        AdminRepository.restoreMember(memberId);
    }
    public void withdrawMember(Long memberId) {
        AdminRepository.withdrawMember(memberId);
    }

    // ── 가게
    public List<AdminDto.StoreRow> getStores(String keyword, String category, int page, int size) {
        return AdminRepository.findAllStores(keyword, category, page * size, size);
    }
    public int countStores(String keyword, String category) {
        return AdminRepository.countAllStores(keyword, category);
    }
    public void deactivateStore(Long storeId, String stopReason) {
        AdminRepository.deactivateStore(storeId, stopReason);
    }
    public void restoreStore(Long storeId) {
        AdminRepository.restoreStore(storeId);
    }

    // ── 주문
    public List<AdminDto.OrderRow> getOrders(String keyword, String orderStatus, int page, int size) {
        return AdminRepository.findAllOrders(keyword, orderStatus, page * size, size);
    }
    public int countOrders(String keyword, String orderStatus) {
        return AdminRepository.countAllOrders(keyword, orderStatus);
    }
    public void updateOrderStatus(Long orderId, String orderStatus) {
        AdminRepository.updateOrderStatus(orderId, orderStatus);
    }

    // ── 리뷰
    public List<AdminDto.ReviewRow> getReviews(String keyword, Double maxRating, int page, int size) {
        return AdminRepository.findAllReviews(keyword, maxRating, page * size, size);
    }
    public int countReviews(String keyword, Double maxRating) {
        return AdminRepository.countAllReviews(keyword, maxRating);
    }
    public void deleteReview(Long reviewId) {
        AdminRepository.deleteReview(reviewId);
    }

    // ── 클레임
    public List<AdminDto.ComplaintRow> getComplaints(String status, String complaintType, int page, int size) {
        return AdminRepository.findAllComplaints(status, complaintType, page * size, size);
    }
    public int countComplaints(String status, String complaintType) {
        return AdminRepository.countAllComplaints(status, complaintType);
    }
    public void replyComplaint(Long complaintId, String adminReply, String status) {
        AdminRepository.updateComplaintReply(complaintId, adminReply, status);
    }
}