package com.demo.mall.order.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class SeckillMetrics {

    private static final String METRIC_TOKEN_TOTAL = "mall.seckill.token.total";
    private static final String METRIC_SUBMIT_TOTAL = "mall.seckill.submit.total";
    private static final String METRIC_ORDER_TOTAL = "mall.seckill.order.total";
    private static final String METRIC_STOCK_INIT_TOTAL = "mall.seckill.stock.init.total";

    private final MeterRegistry meterRegistry;

    public SeckillMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void tokenIssued() {
        meterRegistry.counter(METRIC_TOKEN_TOTAL, "result", "issued").increment();
    }

    public void submitAccepted(String mode) {
        meterRegistry.counter(METRIC_SUBMIT_TOTAL, "result", "accepted", "mode", mode).increment();
    }

    public void submitRejected(String reason) {
        meterRegistry.counter(METRIC_SUBMIT_TOTAL, "result", "rejected", "reason", reason).increment();
    }

    public void submitDuplicate() {
        meterRegistry.counter(METRIC_SUBMIT_TOTAL, "result", "duplicate").increment();
    }

    public void orderCreated(String mode) {
        meterRegistry.counter(METRIC_ORDER_TOTAL, "result", "created", "mode", mode).increment();
    }

    public void orderFailed(String mode) {
        meterRegistry.counter(METRIC_ORDER_TOTAL, "result", "failed", "mode", mode).increment();
    }

    public void stockInitialized() {
        meterRegistry.counter(METRIC_STOCK_INIT_TOTAL).increment();
    }
}
