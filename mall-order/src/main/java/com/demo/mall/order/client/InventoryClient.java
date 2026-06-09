package com.demo.mall.order.client;

import com.demo.mall.common.api.Result;
import com.demo.mall.order.client.dto.StockDeductRequest;
import com.demo.mall.order.client.dto.StockLockRequest;
import com.demo.mall.order.client.dto.StockReleaseRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mall-inventory", path = "/inventory/stock")
public interface InventoryClient {

    @PostMapping("/lock")
    Result<Void> lock(@RequestBody StockLockRequest request);

    @PostMapping("/release")
    Result<Void> release(@RequestBody StockReleaseRequest request);

    @PostMapping("/deduct")
    Result<Void> deduct(@RequestBody StockDeductRequest request);
}
