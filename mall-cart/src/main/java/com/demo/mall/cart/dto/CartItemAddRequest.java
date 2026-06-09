package com.demo.mall.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemAddRequest(
        @NotNull Long skuId,
        @NotNull @Min(1) Integer quantity
) {
}
