package com.demo.mall.order.controller;

import com.demo.mall.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderHealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("mall-order is up");
    }
}
