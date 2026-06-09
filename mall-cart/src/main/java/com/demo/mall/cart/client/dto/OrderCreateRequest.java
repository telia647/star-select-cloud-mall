package com.demo.mall.cart.client.dto;

import java.util.List;

public record OrderCreateRequest(
        List<OrderCreateItemRequest> items,
        String remark,
        String requestId
) {
}
