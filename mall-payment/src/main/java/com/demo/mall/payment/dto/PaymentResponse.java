package com.demo.mall.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String payNo,
        String orderNo,
        Long userId,
        BigDecimal amount,
        Integer status,
        String payChannel,
        LocalDateTime paidAt
) {
}
