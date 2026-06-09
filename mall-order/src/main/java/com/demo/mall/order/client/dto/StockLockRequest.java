package com.demo.mall.order.client.dto;

import java.util.List;

public record StockLockRequest(
        String orderNo,
        List<StockItemRequest> items
) {
}
