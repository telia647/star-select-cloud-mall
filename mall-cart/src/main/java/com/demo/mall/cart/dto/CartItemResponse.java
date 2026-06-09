package com.demo.mall.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
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
