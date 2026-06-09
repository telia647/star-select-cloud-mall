package com.demo.mall.order.client;

import com.demo.mall.common.api.Result;
import com.demo.mall.order.client.dto.SeckillValidateRequest;
import com.demo.mall.order.client.dto.SeckillValidateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "mall-promotion", path = "/promotions/internal/seckill")
public interface PromotionClient {

    @PostMapping("/validate")
    Result<SeckillValidateResponse> validate(@RequestBody SeckillValidateRequest request);
}
