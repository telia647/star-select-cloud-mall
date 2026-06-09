package com.demo.mall.promotion.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PromotionSeckillSkuAdminRequest(
        Long id,
        @NotNull Long activityId,
        @NotNull Long sessionId,
        @NotNull Long skuId,
        @NotNull Long productId,
        @NotBlank @Size(max = 128) String productName,
        @NotBlank @Size(max = 64) String skuCode,
        @Size(max = 255) String subtitle,
        @NotNull @DecimalMin("0.01") BigDecimal originalPrice,
        @NotNull @DecimalMin("0.01") BigDecimal seckillPrice,
        @NotNull @Min(0) Integer totalStock,
        @NotNull @Min(0) Integer availableStock,
        @NotNull @Min(1) Integer limitPerUser,
        @Size(max = 32) String badge,
        @NotNull Integer sort,
        @NotNull Integer status
) {
}
