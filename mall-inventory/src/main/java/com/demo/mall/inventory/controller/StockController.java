package com.demo.mall.inventory.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.inventory.dto.StockDeductRequest;
import com.demo.mall.inventory.dto.StockLockRequest;
import com.demo.mall.inventory.dto.StockReleaseRequest;
import com.demo.mall.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inventory/stock")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping("/lock")
    public Result<Void> lock(@Valid @RequestBody StockLockRequest request) {
        stockService.lock(request);
        return Result.success();
    }

    @PostMapping("/release")
    public Result<Void> release(@Valid @RequestBody StockReleaseRequest request) {
        stockService.release(request.orderNo());
        return Result.success();
    }

    @PostMapping("/deduct")
    public Result<Void> deduct(@Valid @RequestBody StockDeductRequest request) {
        stockService.deduct(request.orderNo());
        return Result.success();
    }
}
