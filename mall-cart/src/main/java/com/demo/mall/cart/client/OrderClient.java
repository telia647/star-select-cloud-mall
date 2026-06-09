package com.demo.mall.cart.client;

import com.demo.mall.cart.client.dto.OrderCreateRequest;
import com.demo.mall.cart.client.dto.OrderCreateResponse;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "mall-order", path = "/orders")
public interface OrderClient {

    @PostMapping
    Result<OrderCreateResponse> create(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                       @RequestBody OrderCreateRequest request);
}
