package com.demo.mall.product.dto;

public record ProductListItemResponse(
        Long id,
        Long categoryId,
        Long shopId,
        String shopName,
        String name,
        String subtitle,
        String mainImage,
        Integer status
) {
}
