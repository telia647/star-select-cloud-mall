package com.demo.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.order.entity.OrderLocalMessage;
import com.demo.mall.order.mapper.OrderLocalMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderLocalMessageService {

    private static final int PENDING = 0;
    private static final int SENT = 1;
    private static final int FAILED = 2;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_BATCH_SIZE = 20;

    private final OrderLocalMessageMapper orderLocalMessageMapper;
    private final ObjectMapper objectMapper;
    private final StreamBridge streamBridge;

    public OrderLocalMessageService(OrderLocalMessageMapper orderLocalMessageMapper,
                                    ObjectMapper objectMapper,
                                    StreamBridge streamBridge) {
        this.orderLocalMessageMapper = orderLocalMessageMapper;
        this.objectMapper = objectMapper;
        this.streamBridge = streamBridge;
    }

    public boolean saveAndSend(String messageKey,
                               String bindingName,
                               String topic,
                               String tag,
                               Object payload) {
        OrderLocalMessage message = new OrderLocalMessage();
        message.setMessageKey(messageKey);
        message.setBindingName(bindingName);
        message.setTopic(topic);
        message.setTag(tag);
        message.setPayload(toJson(payload));
        message.setStatus(PENDING);
        message.setRetryCount(0);
        orderLocalMessageMapper.insert(message);

        boolean sent = false;
        try {
            sent = streamBridge.send(bindingName, payload);
            message.setStatus(sent ? SENT : FAILED);
            message.setSentAt(sent ? LocalDateTime.now() : null);
            message.setLastError(sent ? null : "streamBridge returned false");
        } catch (RuntimeException ex) {
            message.setStatus(FAILED);
            message.setLastError(trimError(ex.getMessage()));
        }
        orderLocalMessageMapper.updateById(message);
        return sent;
    }

    @Scheduled(fixedDelayString = "${mall.local-message.retry-delay-ms:10000}")
    public void retryPendingMessages() {
        List<OrderLocalMessage> messages = orderLocalMessageMapper.selectList(new QueryWrapper<OrderLocalMessage>()
                .in("status", PENDING, FAILED)
                .lt("retry_count", MAX_RETRY_COUNT)
                .orderByAsc("updated_at")
                .last("LIMIT " + RETRY_BATCH_SIZE));

        for (OrderLocalMessage message : messages) {
            retry(message);
        }
    }

    private void retry(OrderLocalMessage message) {
        try {
            boolean sent = streamBridge.send(message.getBindingName(), objectMapper.readTree(message.getPayload()));
            message.setStatus(sent ? SENT : FAILED);
            message.setSentAt(sent ? LocalDateTime.now() : null);
            message.setLastError(sent ? null : "streamBridge returned false");
        } catch (RuntimeException | JsonProcessingException ex) {
            message.setStatus(FAILED);
            message.setLastError(trimError(ex.getMessage()));
        }
        message.setRetryCount(nextRetryCount(message.getRetryCount()));
        orderLocalMessageMapper.updateById(message);
    }

    private int nextRetryCount(Integer retryCount) {
        return retryCount == null ? 1 : retryCount + 1;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "order local message encode failed");
        }
    }

    private String trimError(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
