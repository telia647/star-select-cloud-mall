package com.demo.mall.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.product.dto.CategoryResponse;
import com.demo.mall.product.entity.Category;
import com.demo.mall.product.mapper.CategoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private static final int ENABLED_STATUS = 1;

    private final CategoryMapper categoryMapper;
    private final ProductCacheService productCacheService;

    public CategoryService(CategoryMapper categoryMapper, ProductCacheService productCacheService) {
        this.categoryMapper = categoryMapper;
        this.productCacheService = productCacheService;
    }

    public List<CategoryResponse> listEnabled() {
        ProductCacheService.CacheValue<List<CategoryResponse>> cached = productCacheService.getCategories();
        if (cached.hit()) {
            return cached.value();
        }

        String token = UUID.randomUUID().toString();
        if (!productCacheService.tryLock("product:categories", token)) {
            sleepBriefly();
            cached = productCacheService.getCategories();
            if (cached.hit()) {
                return cached.value();
            }
            return loadEnabledFromDb();
        }

        try {
            cached = productCacheService.getCategories();
            if (cached.hit()) {
                return cached.value();
            }
            List<CategoryResponse> categories = loadEnabledFromDb();
            productCacheService.putCategories(categories);
            return categories;
        } finally {
            productCacheService.unlock("product:categories", token);
        }
    }

    private List<CategoryResponse> loadEnabledFromDb() {
        return categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, ENABLED_STATUS)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId))
                .stream()
                .map(category -> new CategoryResponse(
                        category.getId(),
                        category.getParentId(),
                        category.getName(),
                        category.getSort()))
                .toList();
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
