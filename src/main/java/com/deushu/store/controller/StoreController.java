package com.deushu.store.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/store")
public class StoreController {
   
   @GetMapping("/list")
   public String storeList() {
      
      return "store/storeList";
   }
   
   // 가게 상세 페이지
   @GetMapping("/{storeId}")
   public String storeDetail(@PathVariable("storeId") Long storeId, Model model) {
       model.addAttribute("storeId", storeId);
       return "store/detail";
   }
}