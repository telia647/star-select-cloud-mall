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
        localMessageService.savePending(
                "order:inventory-deducted:" + event.orderNo(),
                "inventoryDeducted-out-0",
                "inventory-deducted-topic",
                "deducted",
                event
        );
        return true;
    }

    public boolean publishSeckillOrder(SeckillOrderEvent event) {
        localMessageService.savePending(
                "order:seckill:" + event.requestId(),
                "seckillOrder-out-0",
                "seckill-order-topic",
                "create",
                event
        );
        return true;
    }
}
