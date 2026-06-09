package com.demo.mall.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record StockDeductRequest(@NotBlank String orderNo) {
}
