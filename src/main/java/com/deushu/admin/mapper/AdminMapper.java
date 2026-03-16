package com.deushu.admin.mapper;

import com.deushu.admin.dto.AdminDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    // ── 대시보드 ──
    AdminDto.Dashboard      findTodayStats();
    List<AdminDto.DailySales>    findWeeklySales();
    List<AdminDto.CategoryStat>  findCategoryOrderStats();

    // ── 회원 ──
    List<AdminDto.MemberRow> findAllMembers(
            @Param("keyword")    String keyword,
            @Param("role")       String role,
            @Param("onlyActive") boolean onlyActive,
            @Param("pageSize")   int pageSize,
            @Param("offset")     int offset);

    int countAllMembers(
            @Param("keyword")    String keyword,
            @Param("role")       String role,
            @Param("onlyActive") boolean onlyActive);

    void updateMemberRole(@Param("id") Long id, @Param("role") String role);
    void updateEsgPoint(  @Param("id") Long id, @Param("esgPoint") int esgPoint);
    void deleteMember(    @Param("id") Long id);

    // ── 가게 ──
    List<AdminDto.StoreRow> findAllStores(
            @Param("keyword")  String keyword,
            @Param("category") String category,
            @Param("status")   String status,
            @Param("pageSize") int pageSize,
            @Param("offset")   int offset);

    void deactivateStore(@Param("id") Long id, @Param("stopReason") String stopReason);    void restoreStore(   @Param("id") Long id);

    // ── 주문 ──
    List<AdminDto.OrderRow> findAllOrders(
            @Param("keyword")  String keyword,
            @Param("status")   String status,
            @Param("pageSize") int pageSize,
            @Param("offset")   int offset);

    void updateOrderStatus(@Param("id") Long id, @Param("status") String status);

    // ── 리뷰 ──
    List<AdminDto.ReviewRow> findAllReviews(
            @Param("keyword")   String keyword,
            @Param("maxRating") Integer maxRating,
            @Param("pageSize")  int pageSize,
            @Param("offset")    int offset);

    void deleteReview(@Param("id") Long id);
}