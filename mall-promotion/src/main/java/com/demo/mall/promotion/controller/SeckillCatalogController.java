package com.demo.mall.promotion.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.promotion.dto.SeckillItemResponse;
import com.demo.mall.promotion.dto.SeckillSessionResponse;
import com.demo.mall.promotion.service.SeckillCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/promotions/seckill")
public class SeckillCatalogController {

    private final SeckillCatalogService seckillCatalogService;

    public SeckillCatalogController(SeckillCatalogService seckillCatalogService) {
        this.seckillCatalogService = seckillCatalogService;
    }

    @GetMapping("/sessions")
    public Result<List<SeckillSessionResponse>> sessions() {
        return Result.success(seckillCatalogService.listSessions());
    }

    @GetMapping("/sessions/{sessionId}/items")
    public Result<List<SeckillItemResponse>> items(@PathVariable("sessionId") Long sessionId) {
        return Result.success(seckillCatalogService.listItems(sessionId));
    }
}
