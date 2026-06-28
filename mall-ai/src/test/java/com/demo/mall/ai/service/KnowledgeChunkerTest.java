package com.demo.mall.ai.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeChunkerTest {

    private final KnowledgeChunker chunker = new KnowledgeChunker();

    @Test
    void splitReturnsEmptyListForBlankContent() {
        assertThat(chunker.split("  ", 700, 100)).isEmpty();
    }

    @Test
    void splitUsesOverlapBetweenChunks() {
        String content = "a".repeat(260);

        List<String> chunks = chunker.split(content, 200, 20);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(200);
        assertThat(chunks.get(1)).hasSize(80);
    }
}
