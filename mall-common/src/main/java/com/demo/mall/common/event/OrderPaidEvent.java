package com.demo.mall.common.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPaidEvent(
        String orderNo,
        String payNo,
        Long userId,
        BigDecimal amount,
        LocalDateTime paidAt
) {
}
