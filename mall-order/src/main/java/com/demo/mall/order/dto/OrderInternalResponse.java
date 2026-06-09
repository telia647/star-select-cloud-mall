package com.demo.mall.order.dto;

import java.math.BigDecimal;

public record OrderInternalResponse(
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        Integer status
) {
}
