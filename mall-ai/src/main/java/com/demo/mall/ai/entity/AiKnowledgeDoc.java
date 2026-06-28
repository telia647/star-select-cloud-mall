package com.demo.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.mall.common.model.BaseEntity;

@TableName("ai_knowledge_doc")
public class AiKnowledgeDoc extends BaseEntity {

    private String title;
    private String category;
    private String content;
    private Integer status;
    private Integer embeddingStatus;
    private String lastEmbeddingError;
    private Long createdBy;
    private Long updatedBy;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getEmbeddingStatus() { return embeddingStatus; }
    public void setEmbeddingStatus(Integer embeddingStatus) { this.embeddingStatus = embeddingStatus; }
    public String getLastEmbeddingError() { return lastEmbeddingError; }
    public void setLastEmbeddingError(String lastEmbeddingError) { this.lastEmbeddingError = lastEmbeddingError; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Long getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(Long updatedBy) { this.updatedBy = updatedBy; }
}
