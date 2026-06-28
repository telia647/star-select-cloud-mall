package com.demo.mall.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.mall.common.model.BaseEntity;

@TableName("ai_message")
public class AiMessage extends BaseEntity {

    private Long conversationId;
    private Long userId;
    private String roleCode;
    private String content;
    private String referencesJson;
    private String toolResultJson;

    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRoleCode() { return roleCode; }
    public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getReferencesJson() { return referencesJson; }
    public void setReferencesJson(String referencesJson) { this.referencesJson = referencesJson; }
    public String getToolResultJson() { return toolResultJson; }
    public void setToolResultJson(String toolResultJson) { this.toolResultJson = toolResultJson; }
}
