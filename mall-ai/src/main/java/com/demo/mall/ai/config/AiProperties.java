package com.demo.mall.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.ai")
public class AiProperties {

    private final Rag rag = new Rag();

    public Rag getRag() {
        return rag;
    }

    public static class Rag {
        private int topK = 5;
        private double similarityThreshold = 0.65;
        private int chunkSize = 700;
        private int chunkOverlap = 100;
        private long retrieveTimeoutMs = 15_000;
        private long toolTimeoutMs = 8_000;

        public int getTopK() { return topK; }
        public void setTopK(int topK) { this.topK = topK; }
        public double getSimilarityThreshold() { return similarityThreshold; }
        public void setSimilarityThreshold(double similarityThreshold) { this.similarityThreshold = similarityThreshold; }
        public int getChunkSize() { return chunkSize; }
        public void setChunkSize(int chunkSize) { this.chunkSize = chunkSize; }
        public int getChunkOverlap() { return chunkOverlap; }
        public void setChunkOverlap(int chunkOverlap) { this.chunkOverlap = chunkOverlap; }
        public long getRetrieveTimeoutMs() { return retrieveTimeoutMs; }
        public void setRetrieveTimeoutMs(long retrieveTimeoutMs) { this.retrieveTimeoutMs = retrieveTimeoutMs; }
        public long getToolTimeoutMs() { return toolTimeoutMs; }
        public void setToolTimeoutMs(long toolTimeoutMs) { this.toolTimeoutMs = toolTimeoutMs; }
    }
}
