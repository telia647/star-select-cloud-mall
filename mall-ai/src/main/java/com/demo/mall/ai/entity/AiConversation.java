package com.demo.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.mall.common.model.BaseEntity;

@TableName("ai_conversation")
public class AiConversation extends BaseEntity {

    private Long userId;
    private String title;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
