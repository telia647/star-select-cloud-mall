package com.demo.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.mall.common.model.BaseEntity;

@TableName("ai_knowledge_chunk")
public class AiKnowledgeChunk extends BaseEntity {

    private Long docId;
    private Integer chunkNo;
    private String content;
    private String vectorId;
    private Integer tokenCount;

    public Long getDocId() { return docId; }
    public void setDocId(Long docId) { this.docId = docId; }
    public Integer getChunkNo() { return chunkNo; }
    public void setChunkNo(Integer chunkNo) { this.chunkNo = chunkNo; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getVectorId() { return vectorId; }
    public void setVectorId(String vectorId) { this.vectorId = vectorId; }
    public Integer getTokenCount() { return tokenCount; }
    public void setTokenCount(Integer tokenCount) { this.tokenCount = tokenCount; }
}
