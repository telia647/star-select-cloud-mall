package com.demo.mall.product.dto;

public record CategoryResponse(
        Long id,
        Long parentId,
        String name,
        Integer sort
) {
}
