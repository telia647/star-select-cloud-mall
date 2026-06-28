package com.demo.mall.ai.dto;

public record AiReferenceResponse(
        Long docId,
        String docTitle,
        String category,
        String content,
        Double score
) {
}
