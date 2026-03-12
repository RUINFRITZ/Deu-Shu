package com.deushu.admin.service;

import com.deushu.admin.dto.AdminDto;
import com.deushu.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;

    // ── 대시보드 ──
    public AdminDto.Dashboard          getDashboardStats()    { return adminMapper.findTodayStats(); }
    public List<AdminDto.DailySales>   getWeeklySales()       { return adminMapper.findWeeklySales(); }
    public List<AdminDto.CategoryStat> getCategoryOrderStats(){ return adminMapper.findCategoryOrderStats(); }

    // ── 회원 ──
    public List<AdminDto.MemberRow> getMembers(String keyword, String role, boolean onlyActive, int page, int size) {
        return adminMapper.findAllMembers(keyword, role, onlyActive, size, page * size);
    }

    @Transactional
    public void updateMemberRole(Long id, String role)    { adminMapper.updateMemberRole(id, role); }
    @Transactional
    public void updateEsgPoint(Long id, int esgPoint)     { adminMapper.updateEsgPoint(id, esgPoint); }
    @Transactional
    public void deleteMember(Long id)                     { adminMapper.deleteMember(id); }

    // ── 가게 ──
    public List<AdminDto.StoreRow> getStores(String keyword, String category, String status, int page, int size) {
        return adminMapper.findAllStores(keyword, category, status, size, page * size);
    }

    @Transactional
    public void deactivateStore(Long id) { adminMapper.deactivateStore(id); }
    @Transactional
    public void restoreStore(Long id)    { adminMapper.restoreStore(id); }

    // ── 주문 ──
    public List<AdminDto.OrderRow> getOrders(String keyword, String status, int page, int size) {
        return adminMapper.findAllOrders(keyword, status, size, page * size);
    }

    @Transactional
    public void updateOrderStatus(Long id, String status) { adminMapper.updateOrderStatus(id, status); }

    // ── 리뷰 ──
    public List<AdminDto.ReviewRow> getReviews(String keyword, Integer maxRating, int page, int size) {
        return adminMapper.findAllReviews(keyword, maxRating, size, page * size);
    }

    @Transactional
    public void deleteReview(Long id) { adminMapper.deleteReview(id); }
}