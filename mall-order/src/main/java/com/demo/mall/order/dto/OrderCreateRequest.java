package com.demo.mall.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderCreateRequest(
        @Valid @NotEmpty List<OrderItemRequest> items,
        String remark,
        @Size(max = 64) String requestId
) {
    public OrderCreateRequest(List<OrderItemRequest> items, String remark) {
        this(items, remark, null);
    }
}
