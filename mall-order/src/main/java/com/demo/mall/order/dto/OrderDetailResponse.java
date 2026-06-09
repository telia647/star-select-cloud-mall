package com.demo.mall.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponse(
        String orderNo,
        Long userId,
        BigDecimal totalAmount,
        Integer status,
        String payNo,
        LocalDateTime payTime,
        LocalDateTime cancelTime,
        LocalDateTime expireTime,
        String remark,
        List<OrderItemResponse> items
) {
}
