package com.demo.mall.product.dto;

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
