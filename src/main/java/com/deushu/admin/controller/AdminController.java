package com.deushu.admin.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.deushu.admin.dto.AdminDto;
import com.deushu.admin.service.AdminService;
import com.deushu.common.response.ApiResponse;

import lombok.RequiredArgsConstructor;

@Controller
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    // ── 페이지 라우팅 ──────────────────────────────────────
    @GetMapping("/admin")
    public String adminIndex() { return "redirect:/admin/dashboard"; }

    @GetMapping("/admin/dashboard")
    public String dashboard() { return "admin/dashboard"; }

    @GetMapping("/admin/members")
    public String members() { return "admin/members"; }

    @GetMapping("/admin/stores")
    public String stores() { return "admin/stores"; }

    @GetMapping("/admin/orders")
    public String orders() { return "admin/orders"; }

    @GetMapping("/admin/reviews")
    public String reviews() { return "admin/reviews"; }

    @GetMapping("/admin/complaints")
    public String complaints() { return "admin/complaints"; }

    private final AdminService adminService;

    // ── 대시보드 API ──────────────────────────────────────
    @ResponseBody
 // 1. 대시보드 상단 통계 (오늘 주문수, 매출 등)
    @GetMapping("/api/admin/dashboard/stats")
    public ApiResponse<?> dashboardStats() {
        return ApiResponse.onSuccess(adminService.getTodayStats());
    }

    // 2. 주간 매출 막대그래프
    @ResponseBody
    @GetMapping("/api/admin/dashboard/weekly-sales")
    public ApiResponse<?> weeklySales() {
        return ApiResponse.onSuccess(adminService.getWeeklySales());
    }

 // 3. 카테고리별 도넛 그래프
    @ResponseBody
    @GetMapping("/api/admin/dashboard/category-stats")
    public ApiResponse<?> categoryStats() {
        return ApiResponse.onSuccess(adminService.getCategoryStats());
    }

    // ── 회원 관리 ──────────────────────────────────────────
    @ResponseBody
    @GetMapping("/api/admin/members")
    public ApiResponse<?> getMembers(
    		@RequestParam(name = "keyword", defaultValue = "") String keyword,
    		@RequestParam(name = "role", defaultValue = "") String role,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "size", defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("data",  adminService.getMembers(keyword, role, page, size));
        result.put("total", adminService.countMembers(keyword, role));
        return ApiResponse.onSuccess(result);
    }

    @ResponseBody
    @PatchMapping("/api/admin/members/role")
    public ApiResponse<?> updateRole(@RequestBody AdminDto.RoleUpdateRequest req) {
        adminService.updateMemberRole(req.getMemberId(), req.getRole());
        return ApiResponse.onSuccess(null);
    }

    @ResponseBody
    @PatchMapping("/api/admin/members/{memberId}/suspend")
    public ApiResponse<?> suspendMember(@PathVariable("memberId") Long memberId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("suspendReason", null) : null;
        adminService.suspendMember(memberId, reason);
        return ApiResponse.onSuccess(null);
    }

    @ResponseBody
    @PatchMapping("/api/admin/members/{memberId}/restore")
    public ApiResponse<?> restoreMember(@PathVariable("memberId") Long memberId) {
        adminService.restoreMember(memberId);
        return ApiResponse.onSuccess(null);
    }

    @ResponseBody
    @PatchMapping("/api/admin/members/{memberId}/withdraw")
    public ApiResponse<?> withdrawMember(@PathVariable("memberId") Long memberId) {
        adminService.withdrawMember(memberId);
        return ApiResponse.onSuccess(null);
    }

    // ── 가게 관리 ──────────────────────────────────────────
    @ResponseBody
    @GetMapping("/api/admin/stores")
    public ApiResponse<?> getStores(
    		@RequestParam(name = "keyword", defaultValue = "") String keyword,
    		@RequestParam(name = "category", defaultValue = "") String category,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "size", defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("data",  adminService.getStores(keyword, category, page, size));
        result.put("total", adminService.countStores(keyword, category));
        return ApiResponse.onSuccess(result);
    }

    @ResponseBody
    @PatchMapping("/api/admin/stores/{storeId}/deactivate")
    public ApiResponse<?> deactivateStore(@PathVariable("storeId") Long storeId,                                          @RequestBody(required = false) java.util.Map<String, String> body) {
        String stopReason = body != null ? body.get("stopReason") : null;
        adminService.deactivateStore(storeId, stopReason);
        return ApiResponse.onSuccess(null);
    }

    @ResponseBody
    @PatchMapping("/api/admin/stores/{storeId}/restore")
    public ApiResponse<?> restoreStore(@PathVariable("storeId") Long storeId) {
        adminService.restoreStore(storeId);
        return ApiResponse.onSuccess(null);
    }

    // ── 주문 관리 ──────────────────────────────────────────
    @ResponseBody
    @GetMapping("/api/admin/orders")
    public ApiResponse<?> getOrders(
    		@RequestParam(name = "keyword", defaultValue = "") String keyword,
    		@RequestParam(name = "orderStatus", defaultValue = "") String orderStatus,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "size", defaultValue = "20") int size) {
        Map<String, Object> result = new HashMap<>();
        result.put("data",  adminService.getOrders(keyword, orderStatus, page, size));
        result.put("total", adminService.countOrders(keyword, orderStatus));
        return ApiResponse.onSuccess(result);
    }

    @ResponseBody
    @PatchMapping("/api/admin/orders/status")
    public ApiResponse<?> updateOrderStatus(@RequestBody AdminDto.OrderStatusRequest req) {
        adminService.updateOrderStatus(req.getOrderId(), req.getOrderStatus());
        return ApiResponse.onSuccess(null);
    }

    // ── 리뷰 관리 ──────────────────────────────────────────
    @ResponseBody
    @GetMapping("/api/admin/reviews")
    public ApiResponse<?> getReviews(
    		@RequestParam(name = "keyword", defaultValue = "") String keyword,
    		@RequestParam(name = "maxRating", required = false) Double maxRating,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "size", defaultValue = "20") int size){
        Map<String, Object> result = new HashMap<>();
        result.put("data",  adminService.getReviews(keyword, maxRating, page, size));
        result.put("total", adminService.countReviews(keyword, maxRating));
        return ApiResponse.onSuccess(result);
    }

    @ResponseBody
    @DeleteMapping("/api/admin/reviews/{reviewId}")
    public ApiResponse<?> deleteReview(@PathVariable("reviewId") Long reviewId) {        adminService.deleteReview(reviewId);
        return ApiResponse.onSuccess(null);
    }

    // ── 클레임 관리 ────────────────────────────────────────
    @ResponseBody
    @GetMapping("/api/admin/complaints")
    public ApiResponse<?> getComplaints(
    		@RequestParam(name = "status", defaultValue = "") String status,
    		@RequestParam(name = "complaintType", defaultValue = "") String complaintType,
    		@RequestParam(name = "page", defaultValue = "0") int page,
    		@RequestParam(name = "size", defaultValue = "20") int size) { 
        Map<String, Object> result = new HashMap<>();
        result.put("data",  adminService.getComplaints(status, complaintType, page, size));
        result.put("total", adminService.countComplaints(status, complaintType));
        return ApiResponse.onSuccess(result);
    }

    @ResponseBody
    @PatchMapping("/api/admin/complaints/reply")
    public ApiResponse<?> replyComplaint(@RequestBody AdminDto.ComplaintReplyRequest req) {
        adminService.replyComplaint(req.getComplaintId(), req.getAdminReply(), req.getStatus());
        return ApiResponse.onSuccess(null);
    }
}