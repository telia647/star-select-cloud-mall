package com.demo.mall.inventory.controller;

import com.demo.mall.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory")
public class InventoryHealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("mall-inventory is up");
    }
}
