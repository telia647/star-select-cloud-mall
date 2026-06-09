package com.demo.mall.order.service;

import com.demo.mall.common.event.InventoryDeductedEvent;
import com.demo.mall.common.event.SeckillOrderEvent;
import org.springframework.stereotype.Service;

@Service
public class OrderEventPublisher {

    private final OrderLocalMessageService localMessageService;

    public OrderEventPublisher(OrderLocalMessageService localMessageService) {
        this.localMessageService = localMessageService;
    }

    public boolean publishInventoryDeducted(InventoryDeductedEvent event) {
        return localMessageService.saveAndSend(
                "order:inventory-deducted:" + event.orderNo(),
                "inventoryDeducted-out-0",
                "inventory-deducted-topic",
                "deducted",
                event
        );
    }

    public boolean publishSeckillOrder(SeckillOrderEvent event) {
        return localMessageService.saveAndSend(
                "order:seckill:" + event.requestId(),
                "seckillOrder-out-0",
                "seckill-order-topic",
                "create",
                event
        );
    }
}
