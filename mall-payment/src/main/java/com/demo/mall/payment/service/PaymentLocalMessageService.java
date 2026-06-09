package com.demo.mall.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.payment.entity.PaymentLocalMessage;
import com.demo.mall.payment.mapper.PaymentLocalMessageMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentLocalMessageService {

    private static final int PENDING = 0;
    private static final int SENT = 1;
    private static final int FAILED = 2;
    private static final int MAX_RETRY_COUNT = 5;
    private static final int RETRY_BATCH_SIZE = 20;

    private final PaymentLocalMessageMapper paymentLocalMessageMapper;
    private final ObjectMapper objectMapper;
    private final StreamBridge streamBridge;

    public PaymentLocalMessageService(PaymentLocalMessageMapper paymentLocalMessageMapper,
                                      ObjectMapper objectMapper,
                                      StreamBridge streamBridge) {
        this.paymentLocalMessageMapper = paymentLocalMessageMapper;
        this.objectMapper = objectMapper;
        this.streamBridge = streamBridge;
    }

    public boolean saveAndSend(String messageKey,
                               String bindingName,
                               String topic,
                               String tag,
                               Object payload) {
        PaymentLocalMessage message = new PaymentLocalMessage();
        message.setMessageKey(messageKey);
        message.setBindingName(bindingName);
        message.setTopic(topic);
        message.setTag(tag);
        message.setPayload(toJson(payload));
        message.setStatus(PENDING);
        message.setRetryCount(0);
        paymentLocalMessageMapper.insert(message);

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
        paymentLocalMessageMapper.updateById(message);
        return sent;
    }

    @Scheduled(fixedDelayString = "${mall.local-message.retry-delay-ms:10000}")
    public void retryPendingMessages() {
        List<PaymentLocalMessage> messages = paymentLocalMessageMapper.selectList(new QueryWrapper<PaymentLocalMessage>()
                .in("status", PENDING, FAILED)
                .lt("retry_count", MAX_RETRY_COUNT)
                .orderByAsc("updated_at")
                .last("LIMIT " + RETRY_BATCH_SIZE));

        for (PaymentLocalMessage message : messages) {
            retry(message);
        }
    }

    private void retry(PaymentLocalMessage message) {
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
        paymentLocalMessageMapper.updateById(message);
    }

    private int nextRetryCount(Integer retryCount) {
        return retryCount == null ? 1 : retryCount + 1;
    }

    private String toJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "payment local message encode failed");
        }
    }

    private String trimError(String message) {
        if (message == null) {
            return "unknown error";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
