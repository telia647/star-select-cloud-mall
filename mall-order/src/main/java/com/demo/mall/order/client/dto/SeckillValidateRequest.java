package com.demo.mall.order.client.dto;

public record SeckillValidateRequest(
        Long activityId,
        Long sessionId,
        Long skuId,
        Integer quantity
) {
}
