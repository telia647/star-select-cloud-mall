package com.demo.mall.inventory.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.context.RoleGuard;
import com.demo.mall.common.security.header.SecurityHeaders;
import com.demo.mall.inventory.dto.StockFlowResponse;
import com.demo.mall.inventory.service.StockFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/inventory/admin")
public class StockAdminController {

    private final StockFlowService stockFlowService;

    public StockAdminController(StockFlowService stockFlowService) {
        this.stockFlowService = stockFlowService;
    }

    @GetMapping("/stock-flows")
    public Result<List<StockFlowResponse>> stockFlows(@RequestHeader(SecurityHeaders.USER_ROLE) String roleCode,
                                                       @RequestParam("orderNo") String orderNo) {
        RoleGuard.requireAdmin(roleCode);
        return Result.success(stockFlowService.listByOrderNo(orderNo));
    }
}
