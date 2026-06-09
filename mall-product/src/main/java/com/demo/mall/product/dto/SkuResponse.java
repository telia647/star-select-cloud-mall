package com.demo.mall.product.dto;

import java.math.BigDecimal;

public record SkuResponse(
        Long id,
        Long productId,
        String skuCode,
        String specJson,
        BigDecimal price,
        Integer status
) {
}
