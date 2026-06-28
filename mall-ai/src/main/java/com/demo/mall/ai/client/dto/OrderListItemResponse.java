package com.demo.mall.ai.client.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderListItemResponse(
        String orderNo,
        BigDecimal totalAmount,
        Integer status,
        String payNo,
        LocalDateTime payTime,
        LocalDateTime cancelTime,
        LocalDateTime expireTime,
        String remark,
        Integer itemCount,
        String firstProductName,
        LocalDateTime createdAt
) {
}
