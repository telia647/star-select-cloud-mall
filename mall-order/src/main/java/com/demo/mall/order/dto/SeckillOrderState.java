package com.demo.mall.order.dto;

public record SeckillOrderState(
        Long userId,
        Long activityId,
        Long sessionId,
        Long seckillSkuId,
        Long skuId,
        Integer quantity,
        String requestId,
        String orderNo,
        String status,
        String message
) {
}
