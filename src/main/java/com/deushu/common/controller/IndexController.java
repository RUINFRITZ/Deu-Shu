package com.deushu.common.controller;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.deushu.store.dto.RegionLocation;
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
    
    // 마이페이지
    @GetMapping("/mypage")
    public String mypage() {
        return "member/mypage";
    }
    
	@GetMapping("/list")
	public String storeList(@RequestParam(name="region") String region,
							@RequestParam(name="radius") double radius,	Model model) {
		
		
		RegionLocation location =  regionService.findLocationByRegionCode(region);
		
		model.addAttribute("centerLat", location.getLat());          // 지도 중심 위도
        model.addAttribute("centerLng", location.getLng());          // 지도 중심 경도
        model.addAttribute("regionName", location.getDisplayName()); // 화면 표시용 이름 (예: 千代田区)
        model.addAttribute("regionCode", region);       			 // 원본 코드 (예: "chiyoda")
        model.addAttribute("r", region);       			 // 원본 코드 (예: "chiyoda")
        model.addAttribute("storePins", storeService.getStorePins(LocalDate.now(), location.getLat(), location.getLng(), radius));
        model.addAttribute("stores",    storeService.getStoresByPage(0, 10, location.getLat(), location.getLng(),radius).getContent());
		return "store/storeList";
	}
}