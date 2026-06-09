package com.demo.mall.promotion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record PromotionSessionAdminRequest(
        Long id,
        @NotNull Long activityId,
        @NotBlank @Size(max = 64) String name,
        @NotNull LocalDateTime startTime,
        @NotNull LocalDateTime endTime,
        @NotNull Integer status,
        @NotNull Integer sort
) {
}
