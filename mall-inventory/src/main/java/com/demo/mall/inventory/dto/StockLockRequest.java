package com.demo.mall.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record StockLockRequest(
        @NotBlank String orderNo,
        @Valid @NotEmpty List<StockItemRequest> items
) {
}
