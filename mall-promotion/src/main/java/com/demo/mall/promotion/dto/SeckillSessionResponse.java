package com.demo.mall.promotion.dto;

import java.time.LocalDateTime;

public record SeckillSessionResponse(
        Long id,
        Long activityId,
        String name,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Integer status,
        String state
) {
}
