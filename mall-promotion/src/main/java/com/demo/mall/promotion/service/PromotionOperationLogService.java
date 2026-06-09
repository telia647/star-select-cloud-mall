package com.demo.mall.promotion.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.promotion.entity.PromotionOperationLog;
import com.demo.mall.promotion.mapper.PromotionOperationLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PromotionOperationLogService {

    private final PromotionOperationLogMapper operationLogMapper;
    private final ObjectMapper objectMapper;

    public PromotionOperationLogService(PromotionOperationLogMapper operationLogMapper,
                                        ObjectMapper objectMapper) {
        this.operationLogMapper = operationLogMapper;
        this.objectMapper = objectMapper;
    }

    public void record(Long operatorId,
                       String operatorName,
                       String roleCode,
                       String action,
                       String resourceType,
                       Long resourceId,
                       Object detail) {
        PromotionOperationLog log = new PromotionOperationLog();
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRoleCode(roleCode);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetail(toJson(detail));
        operationLogMapper.insert(log);
    }

    public List<PromotionOperationLog> recent(int limit) {
        return operationLogMapper.selectList(new LambdaQueryWrapper<PromotionOperationLog>()
                .orderByDesc(PromotionOperationLog::getCreatedAt)
                .last("LIMIT " + Math.min(Math.max(limit, 1), 100)));
    }

    private String toJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"operation detail encode failed\"}";
        }
    }
}
