package com.demo.mall.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProductQueryRequest(
        Long categoryId,
        String keyword,
        @Min(1) Long pageNo,
        @Min(1) @Max(100) Long pageSize
) {

    public long normalizedPageNo() {
        return pageNo == null ? 1 : pageNo;
    }

    public long normalizedPageSize() {
        return pageSize == null ? 10 : pageSize;
    }
}
