package com.demo.mall.order.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.context.RoleGuard;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.order.dto.OrderStatusLogResponse;
import com.demo.mall.order.service.OrderStatusLogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders/admin")
public class OrderAdminController {

    private final OrderStatusLogService orderStatusLogService;

    public OrderAdminController(OrderStatusLogService orderStatusLogService) {
        this.orderStatusLogService = orderStatusLogService;
    }

    @GetMapping("/{orderNo}/status-logs")
    public Result<List<OrderStatusLogResponse>> statusLogs(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                           @PathVariable("orderNo") String orderNo) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(orderStatusLogService.listByOrderNo(orderNo));
    }
}
