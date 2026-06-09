package com.demo.mall.product.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.product.dto.ProductSkuResponse;
import com.demo.mall.product.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products/internal")
public class InternalProductController {

    private final ProductService productService;

    public InternalProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/skus/{skuId}")
    public Result<ProductSkuResponse> getSku(@PathVariable("skuId") Long skuId) {
        return Result.success(productService.getSku(skuId));
    }
}
