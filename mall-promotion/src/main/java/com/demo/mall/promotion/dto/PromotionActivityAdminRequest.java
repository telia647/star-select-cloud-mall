package com.demo.mall.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromotionActivityAdminRequest(
        Long id,
        @NotBlank @Size(max = 128) String name,
        @NotBlank @Size(max = 128) String title,
        @Size(max = 512) String description,
        @NotNull Integer status
) {
}
