package com.demo.mall.payment.client;

import com.demo.mall.common.api.Result;
import com.demo.mall.payment.client.dto.OrderInternalResponse;
import com.demo.mall.payment.client.dto.OrderPaidRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mall-order", path = "/orders/internal")
public interface OrderClient {

    @GetMapping("/{orderNo}")
    Result<OrderInternalResponse> detail(@PathVariable("orderNo") String orderNo);

    @PostMapping("/{orderNo}/paid")
    Result<Void> markPaid(@PathVariable("orderNo") String orderNo, @RequestBody OrderPaidRequest request);
}
