package com.demo.mall.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long skuId,
        Long productId,
        String productName,
        String skuCode,
        String specJson,
        Integer quantity,
        BigDecimal price,
        BigDecimal totalAmount
) {
}
