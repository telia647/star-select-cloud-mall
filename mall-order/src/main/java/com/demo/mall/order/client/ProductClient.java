package com.demo.mall.order.client;

import com.demo.mall.common.api.Result;
import com.demo.mall.order.client.dto.ProductSkuResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "mall-product", path = "/products/internal")
public interface ProductClient {

    @GetMapping("/skus/{skuId}")
    Result<ProductSkuResponse> getSku(@PathVariable("skuId") Long skuId);
}
