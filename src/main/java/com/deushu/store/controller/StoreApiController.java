package com.deushu.store.controller;

import com.deushu.store.dto.StoreDetailResponse;
import com.deushu.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreApiController {

    private final StoreService storeService;

    @GetMapping("/{storeId}")
    public StoreDetailResponse getStoreDetail(@PathVariable("storeId") Long storeId) {
        return storeService.getStoreDetail(storeId);
    }
}
