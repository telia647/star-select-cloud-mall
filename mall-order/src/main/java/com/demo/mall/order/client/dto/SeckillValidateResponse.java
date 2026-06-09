package com.demo.mall.order.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeckillValidateResponse(
        Long seckillSkuId,
        Long activityId,
        Long sessionId,
        Long skuId,
        BigDecimal seckillPrice,
        Integer availableStock,
        Integer limitPerUser,
        LocalDateTime sessionEndTime
) {
}
