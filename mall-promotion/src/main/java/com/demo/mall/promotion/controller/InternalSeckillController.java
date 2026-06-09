package com.demo.mall.promotion.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.promotion.dto.SeckillValidateRequest;
import com.demo.mall.promotion.dto.SeckillValidateResponse;
import com.demo.mall.promotion.service.SeckillCatalogService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/promotions/internal/seckill")
public class InternalSeckillController {

    private final SeckillCatalogService seckillCatalogService;

    public InternalSeckillController(SeckillCatalogService seckillCatalogService) {
        this.seckillCatalogService = seckillCatalogService;
    }

    @PostMapping("/validate")
    public Result<SeckillValidateResponse> validate(@Valid @RequestBody SeckillValidateRequest request) {
        return Result.success(seckillCatalogService.validateForSubmit(request));
    }
}
