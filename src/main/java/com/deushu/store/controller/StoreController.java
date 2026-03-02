package com.deushu.store.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/map")
public class StoreController {
	
	@GetMapping("/list")
	public String storeList() {
		
		return "store/storeList";
	}
	
}
