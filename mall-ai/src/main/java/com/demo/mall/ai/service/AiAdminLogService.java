package com.demo.mall.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.ai.dto.AiLogResponse;
import com.demo.mall.ai.entity.AiModelCallLog;
import com.demo.mall.ai.entity.AiToolCallLog;
import com.demo.mall.ai.mapper.AiModelCallLogMapper;
import com.demo.mall.ai.mapper.AiToolCallLogMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AiAdminLogService {

    private final AiModelCallLogMapper modelCallLogMapper;
    private final AiToolCallLogMapper toolCallLogMapper;

    public AiAdminLogService(AiModelCallLogMapper modelCallLogMapper, AiToolCallLogMapper toolCallLogMapper) {
        this.modelCallLogMapper = modelCallLogMapper;
        this.toolCallLogMapper = toolCallLogMapper;
    }

    public List<AiLogResponse> modelCalls() {
        return modelCallLogMapper.selectList(new LambdaQueryWrapper<AiModelCallLog>()
                        .orderByDesc(AiModelCallLog::getCreatedAt)
                        .last("LIMIT 100"))
                .stream()
                .map(item -> new AiLogResponse(
                        item.getId(),
                        item.getConversationId(),
                        item.getUserId(),
                        item.getModelName(),
                        item.getStatus(),
                        item.getElapsedMs(),
                        item.getErrorMessage(),
                        item.getCreatedAt()
                ))
                .toList();
    }

    public List<AiLogResponse> toolCalls() {
        return toolCallLogMapper.selectList(new LambdaQueryWrapper<AiToolCallLog>()
                        .orderByDesc(AiToolCallLog::getCreatedAt)
                        .last("LIMIT 100"))
                .stream()
                .map(item -> new AiLogResponse(
                        item.getId(),
                        item.getConversationId(),
                        item.getUserId(),
                        item.getToolName(),
                        item.getStatus(),
                        item.getElapsedMs(),
                        item.getErrorMessage(),
                        item.getCreatedAt()
                ))
                .toList();
    }
}
