package com.demo.mall.product.controller;

import com.demo.mall.common.api.Result;
import com.demo.mall.product.dto.CategoryResponse;
import com.demo.mall.product.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public Result<List<CategoryResponse>> list() {
        return Result.success(categoryService.listEnabled());
    }
}
