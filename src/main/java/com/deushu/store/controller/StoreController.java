package com.deushu.store.controller;

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
		return "store/detail";
		
	}

	
}
