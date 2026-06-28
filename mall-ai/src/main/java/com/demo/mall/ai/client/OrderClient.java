package com.demo.mall.ai.client;

import com.demo.mall.ai.client.dto.OrderDetailResponse;
import com.demo.mall.ai.client.dto.OrderListItemResponse;
import com.demo.mall.common.api.PageResult;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.security.header.SecurityHeaders;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "mall-order", path = "/orders")
public interface OrderClient {

    @GetMapping("/{orderNo}")
    Result<OrderDetailResponse> detail(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                       @PathVariable("orderNo") String orderNo);

    @GetMapping("/me")
    Result<PageResult<OrderListItemResponse>> listMine(@RequestHeader(SecurityHeaders.USER_ID) Long userId,
                                                       @RequestParam("pageNo") long pageNo,
                                                       @RequestParam("pageSize") long pageSize);
}
