package com.demo.mall.cart.client.dto;

import java.math.BigDecimal;

public record OrderCreateResponse(
        String orderNo,
        BigDecimal totalAmount,
        Integer status
) {
}
