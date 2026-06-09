package com.demo.mall.payment.client.dto;

import java.math.BigDecimal;

public record OrderInternalResponse(
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        Integer status
) {
}
