package com.demo.mall.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.common.event.OrderPaidEvent;
import com.demo.mall.payment.client.OrderClient;
import com.demo.mall.payment.client.dto.OrderInternalResponse;
import com.demo.mall.payment.client.dto.OrderPaidRequest;
import com.demo.mall.payment.dto.PaymentRequest;
import com.demo.mall.payment.dto.PaymentResponse;
import com.demo.mall.payment.entity.PaymentOrder;
import com.demo.mall.payment.mapper.PaymentOrderMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PaymentService {

    private static final int ORDER_PENDING_PAYMENT = 10;
    private static final int ORDER_PAID = 20;
    private static final int PAYMENT_SUCCESS = 1;
    private static final DateTimeFormatter PAY_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final PaymentOrderMapper paymentOrderMapper;
    private final OrderClient orderClient;
    private final PaymentLocalMessageService localMessageService;

    public PaymentService(PaymentOrderMapper paymentOrderMapper,
                          OrderClient orderClient,
                          PaymentLocalMessageService localMessageService) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.orderClient = orderClient;
        this.localMessageService = localMessageService;
    }

    @Transactional
    public PaymentResponse pay(Long userId, PaymentRequest request) {
        OrderInternalResponse order = getOrder(request.orderNo());
        if (!order.userId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }

        PaymentOrder existing = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getOrderNo, request.orderNo())
                .eq(PaymentOrder::getStatus, PAYMENT_SUCCESS));
        if (existing != null) {
            return toResponse(existing);
        }

        if (!Integer.valueOf(ORDER_PENDING_PAYMENT).equals(order.status())
                && !Integer.valueOf(ORDER_PAID).equals(order.status())) {
            throw new BizException(ErrorCode.PAYMENT_ORDER_INVALID);
        }

        PaymentOrder payment = new PaymentOrder();
        payment.setPayNo(nextPayNo());
        payment.setOrderNo(order.orderNo());
        payment.setUserId(userId);
        payment.setAmount(order.totalAmount());
        payment.setStatus(PAYMENT_SUCCESS);
        payment.setPayChannel(request.payChannel() == null || request.payChannel().isBlank()
                ? "MOCK"
                : request.payChannel());
        payment.setPaidAt(LocalDateTime.now());
        paymentOrderMapper.insert(payment);

        assertSuccess(orderClient.markPaid(order.orderNo(), new OrderPaidRequest(payment.getPayNo())));
        publishOrderPaid(payment);
        return toResponse(payment);
    }

    public PaymentResponse detail(Long userId, String payNo) {
        PaymentOrder payment = paymentOrderMapper.selectOne(new LambdaQueryWrapper<PaymentOrder>()
                .eq(PaymentOrder::getPayNo, payNo));
        if (payment == null) {
            throw new BizException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (!payment.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toResponse(payment);
    }

    private OrderInternalResponse getOrder(String orderNo) {
        Result<OrderInternalResponse> result = orderClient.detail(orderNo);
        assertSuccess(result);
        return result.getData();
    }

    private PaymentResponse toResponse(PaymentOrder payment) {
        return new PaymentResponse(
                payment.getPayNo(),
                payment.getOrderNo(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPayChannel(),
                payment.getPaidAt()
        );
    }

    private String nextPayNo() {
        return "P" + LocalDateTime.now().format(PAY_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private void publishOrderPaid(PaymentOrder payment) {
        localMessageService.saveAndSend(
                "payment:order-paid:" + payment.getPayNo(),
                "orderPaid-out-0",
                "order-paid-topic",
                "paid",
                new OrderPaidEvent(
                        payment.getOrderNo(),
                        payment.getPayNo(),
                        payment.getUserId(),
                        payment.getAmount(),
                        payment.getPaidAt()
                )
        );
    }

    private void assertSuccess(Result<?> result) {
        if (result == null || !result.isSuccess()) {
            String message = result == null ? "remote service call failed" : result.getMessage();
            throw new BizException(ErrorCode.INTERNAL_ERROR, message);
        }
    }
}
