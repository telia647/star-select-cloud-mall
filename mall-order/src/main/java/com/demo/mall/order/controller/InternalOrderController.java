package com.demo.mall.order.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.order.dto.OrderInternalResponse;
import com.demo.mall.order.dto.OrderPaidRequest;
import com.demo.mall.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders/internal")
public class InternalOrderController {

    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping("/{orderNo}")
    public Result<OrderInternalResponse> internalDetail(@PathVariable("orderNo") String orderNo) {
        return Result.success(orderService.internalDetail(orderNo));
    }

    @PostMapping("/{orderNo}/paid")
    public Result<Void> markPaid(@PathVariable("orderNo") String orderNo,
                                 @Valid @RequestBody OrderPaidRequest request) {
        orderService.markPaid(orderNo, request.payNo());
        return Result.success();
    }
}
