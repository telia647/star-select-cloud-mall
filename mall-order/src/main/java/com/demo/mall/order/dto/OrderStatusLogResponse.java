package com.demo.mall.order.dto;

import java.time.LocalDateTime;

public record OrderStatusLogResponse(
        String orderNo,
        Long userId,
        Integer fromStatus,
        Integer toStatus,
        String eventType,
        String bizNo,
        String remark,
        LocalDateTime createdAt
) {
}
