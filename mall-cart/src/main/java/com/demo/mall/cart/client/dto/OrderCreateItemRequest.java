package com.demo.mall.cart.client.dto;

public record OrderCreateItemRequest(
        Long skuId,
        Integer quantity
) {
}
