package com.demo.mall.order.service;

import com.demo.mall.order.entity.MqConsumeLog;
import com.demo.mall.order.mapper.MqConsumeLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MqConsumeLogServiceTest {

    private MqConsumeLogMapper mapper;
    private MqConsumeLogService service;

    @BeforeEach
    void setUp() {
        mapper = mock(MqConsumeLogMapper.class);
        service = new MqConsumeLogService(mapper);
    }

    @Test
    void consumeSkipsActionWhenMessageAlreadySucceeded() {
        MqConsumeLog log = new MqConsumeLog();
        log.setStatus(1);
        when(mapper.selectOne(any())).thenReturn(log);
        Runnable action = mock(Runnable.class);

        service.consume("group", "key", action);

        verify(action, never()).run();
        verify(mapper, never()).updateById(any(MqConsumeLog.class));
    }

    @Test
    void consumeRunsActionAndMarksSuccessForNewMessage() {
        when(mapper.selectOne(any())).thenReturn(null);

        service.consume("group", "key", () -> {
        });

        verify(mapper).insert(any(MqConsumeLog.class));
        ArgumentCaptorHolder.verifySuccessUpdate(mapper);
    }

    private static final class ArgumentCaptorHolder {
        private ArgumentCaptorHolder() {
        }

        static void verifySuccessUpdate(MqConsumeLogMapper mapper) {
            org.mockito.ArgumentCaptor<MqConsumeLog> captor = org.mockito.ArgumentCaptor.forClass(MqConsumeLog.class);
            verify(mapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(1);
        }
    }
}
