package com.demo.mall.order.client.dto;

public record StockItemRequest(
        Long skuId,
        Integer quantity
) {
}
