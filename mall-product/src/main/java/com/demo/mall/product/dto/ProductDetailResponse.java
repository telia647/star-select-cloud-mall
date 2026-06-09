package com.demo.mall.product.dto;

import java.util.List;

public record ProductDetailResponse(
        Long id,
        Long categoryId,
        String name,
        String subtitle,
        Integer status,
        List<SkuResponse> skus
) {
}
