package com.demo.mall.ai.dto;

import java.time.LocalDateTime;

public record AiLogResponse(
        Long id,
        Long conversationId,
        Long userId,
        String name,
        Integer status,
        Long elapsedMs,
        String errorMessage,
        LocalDateTime createdAt
) {
}
