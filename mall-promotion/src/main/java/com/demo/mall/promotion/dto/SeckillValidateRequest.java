package com.demo.mall.promotion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeckillValidateRequest(
        @NotNull Long activityId,
        @NotNull Long sessionId,
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity
) {
}
