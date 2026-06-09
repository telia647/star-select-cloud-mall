package com.demo.mall.common.event;

import java.time.LocalDateTime;

public record InventoryDeductedEvent(
        String orderNo,
        String payNo,
        LocalDateTime deductedAt
) {
}
