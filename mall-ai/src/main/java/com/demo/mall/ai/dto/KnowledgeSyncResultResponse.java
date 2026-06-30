package com.demo.mall.ai.dto;

import java.util.List;

public record KnowledgeSyncResultResponse(
        int created,
        int updated,
        int failed,
        List<KnowledgeDocResponse> items
) {
}
