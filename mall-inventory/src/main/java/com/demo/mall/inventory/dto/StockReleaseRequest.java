package com.demo.mall.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record StockReleaseRequest(@NotBlank String orderNo) {
}
