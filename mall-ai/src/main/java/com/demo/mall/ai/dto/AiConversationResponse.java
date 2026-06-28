package com.demo.mall.ai.dto;

import java.time.LocalDateTime;

public record AiConversationResponse(
        String id,
        String title,
        LocalDateTime updatedAt
) {
}
