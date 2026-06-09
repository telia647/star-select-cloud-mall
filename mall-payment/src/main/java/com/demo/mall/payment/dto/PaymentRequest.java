package com.demo.mall.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentRequest(
        @NotBlank String orderNo,
        String payChannel
) {
}
