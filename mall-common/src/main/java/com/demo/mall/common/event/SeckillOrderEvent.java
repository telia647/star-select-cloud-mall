package com.demo.mall.common.event;

import java.math.BigDecimal;

public record SeckillOrderEvent(
        String requestId,
        Long userId,
        Long activityId,
        Long sessionId,
        Long seckillSkuId,
        BigDecimal seckillPrice,
        Long skuId,
        Integer quantity,
        String remark
) {
}
