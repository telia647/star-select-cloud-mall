package com.demo.mall.ai.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunker {

    public List<String> split(String content, int chunkSize, int overlap) {
        String normalized = content == null ? "" : content.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        int safeChunkSize = Math.max(chunkSize, 200);
        int safeOverlap = Math.max(Math.min(overlap, safeChunkSize / 2), 0);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + safeChunkSize, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(end - safeOverlap, start + 1);
        }
        return chunks;
    }
}
