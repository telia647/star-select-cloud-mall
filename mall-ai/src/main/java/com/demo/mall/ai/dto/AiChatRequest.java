package com.demo.mall.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        Long conversationId,
        @NotBlank String message
) {
}
