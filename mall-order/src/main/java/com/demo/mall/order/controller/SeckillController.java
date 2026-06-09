package com.demo.mall.order.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.context.RoleGuard;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.order.dto.SeckillCreateRequest;
import com.demo.mall.order.dto.SeckillCreateResponse;
import com.demo.mall.order.dto.SeckillStockRequest;
import com.demo.mall.order.dto.SeckillTokenRequest;
import com.demo.mall.order.dto.SeckillTokenResponse;
import com.demo.mall.order.service.SeckillService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping("/stocks")
    public Result<Void> initStock(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                  @Valid @RequestBody SeckillStockRequest request) {
        RoleGuard.requireAdmin(roleCode);
        seckillService.initStock(request);
        return Result.success();
    }

    @PostMapping
    public Result<SeckillCreateResponse> submit(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                @Valid @RequestBody SeckillCreateRequest request) {
        return Result.success(seckillService.submit(userId, request));
    }

    @PostMapping("/tokens")
    public Result<SeckillTokenResponse> issueToken(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                   @Valid @RequestBody SeckillTokenRequest request) {
        return Result.success(seckillService.issueToken(userId, request));
    }

    @GetMapping("/{requestId}")
    public Result<SeckillCreateResponse> result(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                @PathVariable("requestId") String requestId) {
        return Result.success(seckillService.result(userId, requestId));
    }
}
