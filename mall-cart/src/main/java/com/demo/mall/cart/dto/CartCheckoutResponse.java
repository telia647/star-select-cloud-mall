package com.demo.mall.cart.dto;

import java.math.BigDecimal;

public record CartCheckoutResponse(
        String orderNo,
        BigDecimal totalAmount,
        Integer status
) {
}
