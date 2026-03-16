package com.deushu.admin.controller;

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
@RequiredArgsConstructor
public class AdminController {

	private final AdminService adminService;

	// ── 화면 ──
	@GetMapping("/admin")
	public String adminPage() {
	    return "redirect:/admin/dashboard";
	}

	@GetMapping("/admin/dashboard")
	public String adminDashboard() { return "admin/dashboard"; }

	@GetMapping("/admin/members")
	public String adminMembers() { return "admin/members"; }

	@GetMapping("/admin/stores")
	public String adminStores() { return "admin/stores"; }

	@GetMapping("/admin/orders")
	public String adminOrders() { return "admin/orders"; }

	@GetMapping("/admin/reviews")
	public String adminReviews() { return "admin/reviews"; }
	// ── 대시보드 ──
	@GetMapping("/api/admin/dashboard/stats")
	@ResponseBody
	public ApiResponse<?> getDashboardStats() {
		return ApiResponse.onSuccess(adminService.getDashboardStats());
	}

	@GetMapping("/api/admin/dashboard/weekly-sales")
	@ResponseBody
	public ApiResponse<?> getWeeklySales() {
		return ApiResponse.onSuccess(adminService.getWeeklySales());
	}

	@GetMapping("/api/admin/dashboard/category-stats")
	@ResponseBody
	public ApiResponse<?> getCategoryStats() {
		return ApiResponse.onSuccess(adminService.getCategoryOrderStats());
	}

	// ── 회원 ──
	@GetMapping("/api/admin/members")
	@ResponseBody
	public ApiResponse<?> getMembers(@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "role", required = false) String role,
			@RequestParam(name = "onlyActive", defaultValue = "false") boolean onlyActive,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.onSuccess(adminService.getMembers(keyword, role, onlyActive, page, size));
	}

	@PatchMapping("/api/admin/members/{id}/role")
	@ResponseBody
	public ApiResponse<Void> updateMemberRole(@PathVariable Long id, @RequestBody AdminDto.RoleUpdateRequest req) {
		adminService.updateMemberRole(id, req.getRole());
		return ApiResponse.onSuccess(null);
	}

	@PatchMapping("/api/admin/members/{id}/esg-point")
	@ResponseBody
	public ApiResponse<Void> updateEsgPoint(@PathVariable Long id, @RequestBody AdminDto.EsgPointRequest req) {
		adminService.updateEsgPoint(id, req.getEsgPoint());
		return ApiResponse.onSuccess(null);
	}

	@DeleteMapping("/api/admin/members/{id}")
	@ResponseBody
	public ApiResponse<Void> deleteMember(@PathVariable Long id) {
		adminService.deleteMember(id);
		return ApiResponse.onSuccess(null);
	}

	// ── 가게 ──
	@GetMapping("/api/admin/stores")
	@ResponseBody
	public ApiResponse<?> getStores(@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "category", required = false) String category,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.onSuccess(adminService.getStores(keyword, category, status, page, size));
	}

	@DeleteMapping("/api/admin/stores/{id}")
	@ResponseBody
	public ApiResponse<Void> deactivateStore(@PathVariable Long id,
			@RequestBody(required = false) AdminDto.StoreStopRequest req) {
		String reason = (req != null && req.getStopReason() != null) ? req.getStopReason() : "";
		adminService.deactivateStore(id, reason);
		return ApiResponse.onSuccess(null);
	}

	@PatchMapping("/api/admin/stores/{id}/restore")
	@ResponseBody
	public ApiResponse<Void> restoreStore(@PathVariable Long id) {
		adminService.restoreStore(id);
		return ApiResponse.onSuccess(null);
	}

	// ── 주문 ──
	@GetMapping("/api/admin/orders")
	@ResponseBody
	public ApiResponse<?> getOrders(@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "status", required = false) String status,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.onSuccess(adminService.getOrders(keyword, status, page, size));
	}

	@PatchMapping("/api/admin/orders/{id}/status")
	@ResponseBody
	public ApiResponse<Void> updateOrderStatus(@PathVariable Long id, @RequestBody AdminDto.OrderStatusRequest req) {
		adminService.updateOrderStatus(id, req.getStatus());
		return ApiResponse.onSuccess(null);
	}

	// ── 리뷰 ──
	@GetMapping("/api/admin/reviews")
	@ResponseBody
	public ApiResponse<?> getReviews(@RequestParam(name = "keyword", required = false) String keyword,
			@RequestParam(name = "maxRating", required = false) Integer maxRating,
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "20") int size) {
		return ApiResponse.onSuccess(adminService.getReviews(keyword, maxRating, page, size));
	}

	@DeleteMapping("/api/admin/reviews/{id}")
	@ResponseBody
	public ApiResponse<Void> deleteReview(@PathVariable Long id) {
		adminService.deleteReview(id);
		return ApiResponse.onSuccess(null);
	}
}