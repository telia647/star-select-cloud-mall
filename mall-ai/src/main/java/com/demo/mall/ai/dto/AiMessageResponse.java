package com.demo.mall.ai.dto;

import java.time.LocalDateTime;

public record AiMessageResponse(
        String id,
        String roleCode,
        String content,
        String referencesJson,
        String toolResultJson,
        LocalDateTime createdAt
) {
}
