package com.demo.mall.inventory.dto;

import java.time.LocalDateTime;

public record StockFlowResponse(
        String orderNo,
        Long skuId,
        String operation,
        Integer quantity,
        Integer beforeAvailableStock,
        Integer afterAvailableStock,
        Integer beforeLockedStock,
        Integer afterLockedStock,
        LocalDateTime createdAt
) {
}
