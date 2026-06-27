package com.demo.mall.product.dto;

import java.util.List;

public record ProductDetailResponse(
        Long id,
        Long categoryId,
        Long shopId,
        String shopName,
        String name,
        String subtitle,
        String mainImage,
        String galleryImages,
        Integer status,
        List<SkuResponse> skus
) {
}
