package com.demo.mall.payment.service;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.BizException;
import com.demo.mall.payment.client.OrderClient;
import com.demo.mall.payment.client.dto.OrderInternalResponse;
import com.demo.mall.payment.client.dto.OrderPaidRequest;
import com.demo.mall.payment.config.PaymentProperties;
import com.demo.mall.payment.dto.PaymentCallbackRequest;
import com.demo.mall.payment.dto.PaymentResponse;
import com.demo.mall.payment.entity.PaymentOrder;
import com.demo.mall.payment.mapper.PaymentOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentOrderMapper paymentOrderMapper;
    private OrderClient orderClient;
    private PaymentLocalMessageService localMessageService;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        orderClient = mock(OrderClient.class);
        localMessageService = mock(PaymentLocalMessageService.class);
        PaymentProperties properties = new PaymentProperties();
        properties.setCallbackSecret("test-callback-secret");
        paymentService = new PaymentService(paymentOrderMapper, orderClient, localMessageService, properties);
    }

    @Test
    void callbackRejectsInvalidSignature() {
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                "O1001",
                "T1001",
                new BigDecimal("99.00"),
                "SUCCESS",
                "bad-signature"
        );

        assertThatThrownBy(() -> paymentService.callback(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("signature invalid");

        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
        verify(orderClient, never()).markPaid(any(), any());
    }

    @Test
    void callbackCreatesPaymentAndMarksOrderPaid() {
        when(paymentOrderMapper.selectOne(any())).thenReturn(null);
        when(orderClient.detail("O1001")).thenReturn(Result.success(new OrderInternalResponse(
                "O1001",
                1L,
                new BigDecimal("99.00"),
                10
        )));
        when(orderClient.markPaid(any(), any())).thenReturn(Result.success());

        String signature = paymentService.signCallback("O1001", "T1001", new BigDecimal("99.00"), "SUCCESS");
        PaymentResponse response = paymentService.callback(new PaymentCallbackRequest(
                "O1001",
                "T1001",
                new BigDecimal("99.00"),
                "SUCCESS",
                signature
        ));

        ArgumentCaptor<PaymentOrder> paymentCaptor = ArgumentCaptor.forClass(PaymentOrder.class);
        ArgumentCaptor<OrderPaidRequest> paidCaptor = ArgumentCaptor.forClass(OrderPaidRequest.class);
        verify(paymentOrderMapper).insert(paymentCaptor.capture());
        verify(orderClient).markPaid(any(), paidCaptor.capture());
        verify(localMessageService).savePending(any(), any(), any(), any(), any());

        assertThat(response.payNo()).isEqualTo("T1001");
        assertThat(response.amount()).isEqualByComparingTo("99.00");
        assertThat(paymentCaptor.getValue().getPayChannel()).isEqualTo("MOCK_CALLBACK");
        assertThat(paidCaptor.getValue().payNo()).isEqualTo("T1001");
    }

    @Test
    void callbackIsIdempotentWhenPaymentAlreadySucceeded() {
        PaymentOrder existing = new PaymentOrder();
        existing.setPayNo("T1001");
        existing.setOrderNo("O1001");
        existing.setUserId(1L);
        existing.setAmount(new BigDecimal("99.00"));
        existing.setStatus(1);
        existing.setPayChannel("MOCK_CALLBACK");
        when(paymentOrderMapper.selectOne(any())).thenReturn(existing);

        String signature = paymentService.signCallback("O1001", "T1001", new BigDecimal("99.00"), "SUCCESS");
        PaymentResponse response = paymentService.callback(new PaymentCallbackRequest(
                "O1001",
                "T1001",
                new BigDecimal("99.00"),
                "SUCCESS",
                signature
        ));

        assertThat(response.payNo()).isEqualTo("T1001");
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
        verify(orderClient, never()).markPaid(any(), any());
    }
}
