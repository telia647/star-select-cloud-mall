package com.demo.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.order.entity.MqConsumeLog;
import com.demo.mall.order.mapper.MqConsumeLogMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MqConsumeLogService {

    private static final int PROCESSING = 0;
    private static final int SUCCESS = 1;
    private static final int FAILED = 2;

    private final MqConsumeLogMapper mqConsumeLogMapper;

    public MqConsumeLogService(MqConsumeLogMapper mqConsumeLogMapper) {
        this.mqConsumeLogMapper = mqConsumeLogMapper;
    }

    @Transactional
    public void consume(String consumerGroup, String messageKey, Runnable action) {
        MqConsumeLog log = get(consumerGroup, messageKey);
        if (log != null && Integer.valueOf(SUCCESS).equals(log.getStatus())) {
            return;
        }
        if (log != null && Integer.valueOf(PROCESSING).equals(log.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "message is being consumed");
        }
        if (log == null) {
            log = createProcessingLog(consumerGroup, messageKey);
            if (Integer.valueOf(SUCCESS).equals(log.getStatus())) {
                return;
            }
        } else {
            log.setStatus(PROCESSING);
            log.setLastError(null);
            mqConsumeLogMapper.updateById(log);
        }

        try {
            action.run();
            log.setStatus(SUCCESS);
            log.setLastError(null);
        } catch (RuntimeException ex) {
            log.setStatus(FAILED);
            log.setRetryCount(nextRetryCount(log.getRetryCount()));
            log.setLastError(trimError(ex.getMessage()));
            mqConsumeLogMapper.updateById(log);
            throw ex;
        }
        mqConsumeLogMapper.updateById(log);
    }

    private MqConsumeLog createProcessingLog(String consumerGroup, String messageKey) {
        MqConsumeLog log = new MqConsumeLog();
        log.setConsumerGroup(consumerGroup);
        log.setMessageKey(messageKey);
        log.setStatus(PROCESSING);
        log.setRetryCount(0);
        try {
            mqConsumeLogMapper.insert(log);
            return log;
        } catch (DuplicateKeyException ex) {
            MqConsumeLog existing = get(consumerGroup, messageKey);
            if (existing != null && Integer.valueOf(SUCCESS).equals(existing.getStatus())) {
                return existing;
            }
            throw new BizException(ErrorCode.CONFLICT, "message is being consumed");
        }
    }

    private MqConsumeLog get(String consumerGroup, String messageKey) {
        return mqConsumeLogMapper.selectOne(new LambdaQueryWrapper<MqConsumeLog>()
                .eq(MqConsumeLog::getConsumerGroup, consumerGroup)
                .eq(MqConsumeLog::getMessageKey, messageKey));
    }

    private int nextRetryCount(Integer retryCount) {
        return retryCount == null ? 1 : retryCount + 1;
    }

    private String trimError(String message) {
        if (message == null || message.isBlank()) {
            return "message consume failed";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
