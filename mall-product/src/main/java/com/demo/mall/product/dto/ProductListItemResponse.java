package com.demo.mall.product.dto;

public record ProductListItemResponse(
        Long id,
        Long categoryId,
        String name,
        String subtitle,
        Integer status
) {
}
