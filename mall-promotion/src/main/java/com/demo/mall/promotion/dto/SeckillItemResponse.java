package com.demo.mall.promotion.dto;

import java.math.BigDecimal;

public record SeckillItemResponse(
        Long id,
        Long activityId,
        Long sessionId,
        Long skuId,
        Long productId,
        String productName,
        String skuCode,
        String subtitle,
        BigDecimal originalPrice,
        BigDecimal seckillPrice,
        Integer totalStock,
        Integer availableStock,
        Integer soldPercent,
        Integer limitPerUser,
        String badge,
        Integer status,
        String state
) {
}
