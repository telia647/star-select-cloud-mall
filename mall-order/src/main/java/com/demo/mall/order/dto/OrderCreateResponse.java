package com.demo.mall.order.dto;

import java.math.BigDecimal;

public record OrderCreateResponse(
        String orderNo,
        BigDecimal totalAmount,
        Integer status
) {
}
