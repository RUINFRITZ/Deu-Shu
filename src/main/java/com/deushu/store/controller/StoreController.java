package com.deushu.store.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/store")
public class StoreController {

    @GetMapping("/{storeId}")
    public String detailPage(@PathVariable("storeId") long storeId, Model model) {
        model.addAttribute("storeId", storeId);

        // 로그인된 경우 memberId를 모델에 추가 (리뷰 삭제 버튼 본인 판별용)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long) {
            model.addAttribute("sessionMemberId", auth.getPrincipal());
        } else {
            model.addAttribute("sessionMemberId", null);
        }

        return "store/detail";
    }
}