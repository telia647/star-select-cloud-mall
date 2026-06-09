package com.demo.mall.promotion.dto;

public record PromotionOperatorContext(
        Long userId,
        String username,
        String roleCode
) {
}
