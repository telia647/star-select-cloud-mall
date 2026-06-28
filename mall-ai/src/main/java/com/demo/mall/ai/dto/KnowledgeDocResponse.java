package com.demo.mall.ai.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record KnowledgeDocResponse(
        @JsonSerialize(using = ToStringSerializer.class) Long id,
        String title,
        String category,
        String content,
        Integer status,
        Integer embeddingStatus,
        String lastEmbeddingError,
        LocalDateTime updatedAt
) {
}
