package com.demo.mall.order.dto;

public record OrderCancelResponse(
        String orderNo,
        Integer status
) {
}
