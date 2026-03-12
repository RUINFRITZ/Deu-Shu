package com.deushu.common.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.deushu.store.dto.RegionLocation;
import com.deushu.store.dto.StoreFilterRequest;
import com.deushu.store.service.RegionService;
import com.deushu.store.service.StoreService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class IndexController {
	
	private final RegionService regionService;
	private final StoreService storeService;
	
    @GetMapping
	public String index(){
	return	"index";
	}
    
    @GetMapping("/owner")
    public String ownerPage() {
        return "member/ownerPage";  // templates/member/ownerPage.html 을 렌더링
    }
    
    // 마이페이지
    @GetMapping("/mypage")
    public String mypage() {
        return "member/mypage";
    }
    
    @GetMapping("/list")
    public String storeList(@RequestParam(name = "region") String region,
                            @RequestParam(name = "radius") double radius,
                            Model model) {

        RegionLocation location = regionService.findLocationByRegionCode(region);

        StoreFilterRequest filter = new StoreFilterRequest();
        filter.setCenterLat(location.getLat());
        filter.setCenterLng(location.getLng());
        filter.setRadius(radius);

        model.addAttribute("centerLat", location.getLat());
        model.addAttribute("centerLng", location.getLng());
        model.addAttribute("regionName", location.getDisplayName());
        model.addAttribute("regionCode", region);
        model.addAttribute("r", region);
        model.addAttribute("radius", radius);

        model.addAttribute("storePins",
                storeService.getStorePins(LocalDate.now(), location.getLat(), location.getLng(), radius));

        model.addAttribute("stores",
                storeService.getStoresByPage(0, 10, filter).getContent());

        return "store/storeList";
    }
}