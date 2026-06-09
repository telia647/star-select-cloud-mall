package com.demo.mall.order.dto;

public record SeckillCreateResponse(
        String requestId,
        String orderNo,
        String status,
        String message
) {
}
