package com.demo.mall.order.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderPaidRequest(@NotBlank String payNo) {
}
