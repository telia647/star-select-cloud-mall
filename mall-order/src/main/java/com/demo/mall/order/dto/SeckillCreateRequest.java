package com.demo.mall.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SeckillCreateRequest(
        @NotNull Long activityId,
        @NotNull Long sessionId,
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity,
        @NotBlank @Size(max = 128) String token,
        @Size(max = 64) String requestId
) {
}
