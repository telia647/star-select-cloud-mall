package com.demo.mall.user.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MemberCouponResponse(
        Long id,
        String couponName,
        String couponType,
        BigDecimal discountAmount,
        BigDecimal thresholdAmount,
        Integer status,
        LocalDateTime validFrom,
        LocalDateTime validTo
) {
}
