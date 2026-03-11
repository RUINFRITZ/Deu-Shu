package com.deushu.common.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.deushu.order.mapper.OrderMapper;
import com.deushu.store.dto.RegionLocation;
import com.deushu.store.service.RegionService;
import com.deushu.store.service.StoreService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexController {

    private final RegionService regionService;
    private final StoreService storeService;
    private final OrderMapper orderMapper;

    // 모든 페이지에서 공통으로 hasCompletedOrder를 model에 추가하는 메서드
    // 각 매핑 메서드에서 호출해서 사용
    private void addCommonAttributes(HttpSession session, Model model) {
        try {
            Long memberId = (Long) session.getAttribute("memberId");
            boolean hasCompletedOrder = false;

            if (memberId != null) {
                // 오늘 결제 완료(PAYMENT_COMPLETED) 주문이 1건 이상이면 true
                hasCompletedOrder = orderMapper.countTodayCompletedOrders(memberId) > 0;
            }

            model.addAttribute("hasCompletedOrder", hasCompletedOrder);

        } catch (Exception e) {
            // DB 조회 실패 시 페이지가 오류나지 않도록 false 반환
            log.warn("hasCompletedOrder 조회 실패: {}", e.getMessage());
            model.addAttribute("hasCompletedOrder", false);
        }
    }

    @GetMapping
    public String index(HttpSession session, Model model) {
        addCommonAttributes(session, model);
        return "index";
    }

    // 마이페이지
    @GetMapping("/mypage")
    public String mypage(HttpSession session, Model model) {
        addCommonAttributes(session, model);
        return "member/mypage";
    }

    @GetMapping("/list")
    public String storeList(@RequestParam(name = "region") String region,
                            @RequestParam(name = "radius") double radius,
                            HttpSession session, Model model) {

        addCommonAttributes(session, model);

        RegionLocation location = regionService.findLocationByRegionCode(region);

        model.addAttribute("centerLat", location.getLat());          // 지도 중심 위도
        model.addAttribute("centerLng", location.getLng());          // 지도 중심 경도
        model.addAttribute("regionName", location.getDisplayName()); // 화면 표시용 이름 (예: 千代田区)
        model.addAttribute("regionCode", region);                    // 원본 코드 (예: "chiyoda")
        model.addAttribute("r", region);                             // 원본 코드 (예: "chiyoda")
        model.addAttribute("storePins", storeService.getStorePins(LocalDate.now(), location.getLat(), location.getLng(), radius));
        model.addAttribute("stores",    storeService.getStoresByPage(0, 10, location.getLat(), location.getLng(), radius).getContent());
        return "store/storeList";
    }
}