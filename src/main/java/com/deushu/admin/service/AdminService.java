package com.deushu.admin.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.deushu.admin.dto.AdminDto;
import com.deushu.admin.mapper.AdminRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;

    // ── 대시보드
    public AdminDto.Dashboard getTodayStats() {
        return adminRepository.findTodayStats();
    }
    public List<AdminDto.DailySales> getWeeklySales() {
        return adminRepository.findWeeklySales();
    }
    public List<AdminDto.CategoryStat> getCategoryStats() {
        return adminRepository.findCategoryOrderStats();
    }

    // ── 회원
    public List<AdminDto.MemberRow> getMembers(String keyword, String role, int page, int size) {
        return adminRepository.findAllMembers(keyword, role, page * size, size);
    }
    public int countMembers(String keyword, String role) {
        return adminRepository.countAllMembers(keyword, role);
    }
    public void updateMemberRole(Long memberId, String role) {
        adminRepository.updateMemberRole(memberId, role);
    }
    public void suspendMember(Long memberId, String suspendReason) {
        adminRepository.suspendMember(memberId);
    }
    public void restoreMember(Long memberId) {
        adminRepository.restoreMember(memberId);
    }
    public void withdrawMember(Long memberId) {
        adminRepository.withdrawMember(memberId);
    }

    // ── 가게
    public List<AdminDto.StoreRow> getStores(String keyword, String category, int page, int size) {
        return adminRepository.findAllStores(keyword, category, page * size, size);
    }
    public int countStores(String keyword, String category) {
        return adminRepository.countAllStores(keyword, category);
    }
    public void deactivateStore(Long storeId, String stopReason) {
        adminRepository.deactivateStore(storeId, stopReason);
    }
    public void restoreStore(Long storeId) {
        adminRepository.restoreStore(storeId);
    }

    // ── 주문
    public List<AdminDto.OrderRow> getOrders(String keyword, String orderStatus, int page, int size) {
        return adminRepository.findAllOrders(keyword, orderStatus, page * size, size);
    }
    public int countOrders(String keyword, String orderStatus) {
        return adminRepository.countAllOrders(keyword, orderStatus);
    }
    public void updateOrderStatus(Long orderId, String orderStatus) {
        adminRepository.updateOrderStatus(orderId, orderStatus);
    }

    // ── 리뷰
    public List<AdminDto.ReviewRow> getReviews(String keyword, Double maxRating, int page, int size) {
        return adminRepository.findAllReviews(keyword, maxRating, page * size, size);
    }
    public int countReviews(String keyword, Double maxRating) {
        return adminRepository.countAllReviews(keyword, maxRating);
    }
    public void deleteReview(Long reviewId) {
        adminRepository.deleteReview(reviewId);
    }

    // ── 클레임
    public List<AdminDto.ComplaintRow> getComplaints(String status, String complaintType, int page, int size) {
        return adminRepository.findAllComplaints(status, complaintType, page * size, size);
    }
    public int countComplaints(String status, String complaintType) {
        return adminRepository.countAllComplaints(status, complaintType);
    }
    public void replyComplaint(Long complaintId, String adminReply, String status) {
        adminRepository.updateComplaintReply(complaintId, adminReply, status);
    }
}