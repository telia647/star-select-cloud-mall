package com.demo.mall.cart.dto;

import jakarta.validation.constraints.Size;

public record CartCheckoutRequest(
        String remark,
        @Size(max = 64) String requestId
) {
}
