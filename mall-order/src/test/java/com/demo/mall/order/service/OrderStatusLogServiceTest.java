package com.demo.mall.order.service;

import com.demo.mall.order.entity.OrderStatusLog;
import com.demo.mall.order.mapper.OrderStatusLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderStatusLogServiceTest {

    private OrderStatusLogMapper orderStatusLogMapper;
    private OrderStatusLogService service;

    @BeforeEach
    void setUp() {
        orderStatusLogMapper = mock(OrderStatusLogMapper.class);
        service = new OrderStatusLogService(orderStatusLogMapper);
    }

    @Test
    void recordTrimsLongRemark() {
        String longRemark = "x".repeat(300);

        service.record("O1001", 1L, null, 10, OrderStatusLogService.EVENT_CREATE, "req-1", longRemark);

        ArgumentCaptor<OrderStatusLog> captor = ArgumentCaptor.forClass(OrderStatusLog.class);
        verify(orderStatusLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getRemark()).hasSize(255);
    }

    @Test
    void listByOrderNoMapsResponses() {
        OrderStatusLog log = new OrderStatusLog();
        log.setOrderNo("O1001");
        log.setUserId(1L);
        log.setFromStatus(10);
        log.setToStatus(20);
        log.setEventType(OrderStatusLogService.EVENT_PAY_SUCCESS);
        log.setBizNo("P1001");
        log.setRemark("payment success");
        log.setCreatedAt(LocalDateTime.now());
        when(orderStatusLogMapper.selectList(any())).thenReturn(List.of(log));

        var responses = service.listByOrderNo("O1001");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).eventType()).isEqualTo(OrderStatusLogService.EVENT_PAY_SUCCESS);
        assertThat(responses.get(0).bizNo()).isEqualTo("P1001");
    }
}
