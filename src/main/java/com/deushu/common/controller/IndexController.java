package com.deushu.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    // 메인 페이지
    @GetMapping("/")
    public String index() {
        return "index";
    }

    // 마이페이지
    @GetMapping("/mypage")
    public String mypage() {
        return "member/mypage";
    }
}