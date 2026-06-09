package com.demo.mall.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record StockItemRequest(
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity
) {
}
