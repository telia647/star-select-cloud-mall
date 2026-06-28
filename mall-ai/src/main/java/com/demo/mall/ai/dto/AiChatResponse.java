package com.demo.mall.ai.dto;

import java.util.List;

public record AiChatResponse(
        String conversationId,
        String answer,
        List<AiReferenceResponse> references,
        Object toolResult
) {
}
