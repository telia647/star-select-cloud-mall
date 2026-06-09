package com.demo.mall.order.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.order.dto.OrderCancelResponse;
import com.demo.mall.order.dto.OrderCreateRequest;
import com.demo.mall.order.dto.OrderCreateResponse;
import com.demo.mall.order.dto.OrderDetailResponse;
import com.demo.mall.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Result<OrderCreateResponse> create(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                              @Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.create(userId, request));
    }

    @GetMapping("/{orderNo}")
    public Result<OrderDetailResponse> detail(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                              @PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.detailForUser(userId, orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    public Result<OrderCancelResponse> cancel(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                              @PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.cancel(userId, orderNo));
    }
}
