package com.demo.mall.order.config;

import com.demo.mall.common.event.OrderPaidEvent;
import com.demo.mall.common.event.SeckillOrderEvent;
import com.demo.mall.order.service.MqConsumeLogService;
import com.demo.mall.order.service.OrderService;
import com.demo.mall.order.service.SeckillService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class OrderEventConsumerConfiguration {

    @Bean
    public Consumer<OrderPaidEvent> orderPaidConsumer(OrderService orderService,
                                                     MqConsumeLogService mqConsumeLogService) {
        return event -> mqConsumeLogService.consume(
                "mall-order:order-paid",
                "order-paid:" + event.payNo(),
                () -> orderService.markPaid(event.orderNo(), event.payNo())
        );
    }

    @Bean
    public Consumer<SeckillOrderEvent> seckillOrderConsumer(SeckillService seckillService,
                                                            MqConsumeLogService mqConsumeLogService) {
        return event -> mqConsumeLogService.consume(
                "mall-order:seckill-order",
                "seckill-order:" + event.requestId(),
                () -> seckillService.consume(event)
        );
    }
}
