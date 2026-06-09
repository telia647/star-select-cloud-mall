package com.demo.mall.product.controller;

import com.demo.mall.common.api.PageResult;
import com.demo.mall.common.api.Result;
import com.demo.mall.product.dto.ProductDetailResponse;
import com.demo.mall.product.dto.ProductListItemResponse;
import com.demo.mall.product.dto.ProductQueryRequest;
import com.demo.mall.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public Result<PageResult<ProductListItemResponse>> page(@Valid ProductQueryRequest request) {
        return Result.success(productService.page(request));
    }

    @GetMapping("/{id}")
    public Result<ProductDetailResponse> detail(@PathVariable("id") Long id) {
        return Result.success(productService.detail(id));
    }
}
