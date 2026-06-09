package com.demo.mall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeckillStockRequest(
        @NotNull Long activityId,
        @NotNull Long sessionId,
        @NotNull Long skuId,
        @NotNull @Min(0) Integer quantity
) {
}
