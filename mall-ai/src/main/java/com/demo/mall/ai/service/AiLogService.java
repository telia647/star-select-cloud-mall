package com.demo.mall.ai.service;

import com.demo.mall.ai.entity.AiModelCallLog;
import com.demo.mall.ai.entity.AiToolCallLog;
import com.demo.mall.ai.mapper.AiModelCallLogMapper;
import com.demo.mall.ai.mapper.AiToolCallLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AiLogService {

    public static final int SUCCESS = 1;
    public static final int FAILED = 2;

    private final AiModelCallLogMapper modelCallLogMapper;
    private final AiToolCallLogMapper toolCallLogMapper;

    public AiLogService(AiModelCallLogMapper modelCallLogMapper, AiToolCallLogMapper toolCallLogMapper) {
        this.modelCallLogMapper = modelCallLogMapper;
        this.toolCallLogMapper = toolCallLogMapper;
    }

    public void modelCall(Long conversationId, Long userId, String modelName, String prompt,
                          String answer, long elapsedMs, String errorMessage) {
        AiModelCallLog log = new AiModelCallLog();
        log.setConversationId(conversationId);
        log.setUserId(userId);
        log.setProvider("deepseek");
        log.setModelName(modelName);
        log.setPrompt(trim(prompt, 16000));
        log.setAnswer(trim(answer, 16000));
        log.setStatus(errorMessage == null ? SUCCESS : FAILED);
        log.setElapsedMs(elapsedMs);
        log.setErrorMessage(trim(errorMessage, 500));
        modelCallLogMapper.insert(log);
    }

    public void toolCall(Long conversationId, Long userId, String toolName, String argumentsJson,
                         String resultJson, long elapsedMs, String errorMessage) {
        AiToolCallLog log = new AiToolCallLog();
        log.setConversationId(conversationId);
        log.setUserId(userId);
        log.setToolName(toolName);
        log.setArgumentsJson(argumentsJson);
        log.setResultJson(resultJson);
        log.setStatus(errorMessage == null ? SUCCESS : FAILED);
        log.setElapsedMs(elapsedMs);
        log.setErrorMessage(trim(errorMessage, 500));
        toolCallLogMapper.insert(log);
    }

    private String trim(String value, int limit) {
        if (value == null || value.length() <= limit) {
            return value;
        }
        return value.substring(0, limit);
    }
}
