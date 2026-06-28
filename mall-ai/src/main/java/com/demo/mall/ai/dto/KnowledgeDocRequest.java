package com.demo.mall.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeDocRequest(
        @NotBlank String title,
        @NotBlank String category,
        @NotBlank String content,
        Integer status
) {
}
