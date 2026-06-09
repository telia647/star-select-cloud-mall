package com.demo.mall.cart.client.dto;

import java.math.BigDecimal;

public record ProductSkuResponse(
        Long skuId,
        Long productId,
        String productName,
        String skuCode,
        String specJson,
        BigDecimal price,
        Integer status
) {
}
