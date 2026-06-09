package com.demo.mall.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.demo.mall.common.api.Result;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.common.event.InventoryDeductedEvent;
import com.demo.mall.order.client.InventoryClient;
import com.demo.mall.order.client.ProductClient;
import com.demo.mall.order.client.dto.ProductSkuResponse;
import com.demo.mall.order.client.dto.StockDeductRequest;
import com.demo.mall.order.client.dto.StockItemRequest;
import com.demo.mall.order.client.dto.StockLockRequest;
import com.demo.mall.order.client.dto.StockReleaseRequest;
import com.demo.mall.order.dto.OrderCancelResponse;
import com.demo.mall.order.dto.OrderCreateRequest;
import com.demo.mall.order.dto.OrderCreateResponse;
import com.demo.mall.order.dto.OrderDetailResponse;
import com.demo.mall.order.dto.OrderInternalResponse;
import com.demo.mall.order.dto.OrderItemRequest;
import com.demo.mall.order.dto.OrderItemResponse;
import com.demo.mall.order.entity.Order;
import com.demo.mall.order.entity.OrderItem;
import com.demo.mall.order.mapper.OrderItemMapper;
import com.demo.mall.order.mapper.OrderMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final int PENDING_PAYMENT = 10;
    private static final int PAID = 20;
    private static final int CANCELED = 30;
    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher orderEventPublisher;
    private final SeckillReservationService seckillReservationService;
    private final OrderStatusLogService orderStatusLogService;
    private final long payTimeoutMinutes;
    private final int expireScanBatchSize;

    public OrderService(OrderMapper orderMapper,
                        OrderItemMapper orderItemMapper,
                        ProductClient productClient,
                        InventoryClient inventoryClient,
                        OrderEventPublisher orderEventPublisher,
                        SeckillReservationService seckillReservationService,
                        OrderStatusLogService orderStatusLogService,
                        @Value("${mall.order.pay-timeout-minutes:30}") long payTimeoutMinutes,
                        @Value("${mall.order.expire-scan-batch-size:50}") int expireScanBatchSize) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productClient = productClient;
        this.inventoryClient = inventoryClient;
        this.orderEventPublisher = orderEventPublisher;
        this.seckillReservationService = seckillReservationService;
        this.orderStatusLogService = orderStatusLogService;
        this.payTimeoutMinutes = payTimeoutMinutes;
        this.expireScanBatchSize = expireScanBatchSize;
    }

    @Transactional
    public OrderCreateResponse create(Long userId, OrderCreateRequest request) {
        String requestId = normalizeRequestId(request.requestId());
        if (requestId != null) {
            Order existing = findOrderByRequestId(userId, requestId);
            if (existing != null) {
                return toCreateResponse(existing);
            }
        }

        Map<Long, Integer> quantityBySku = aggregateItems(request.items());
        String orderNo = nextOrderNo();

        List<OrderItem> items = quantityBySku.entrySet()
                .stream()
                .map(entry -> buildOrderItem(orderNo, entry.getKey(), entry.getValue()))
                .toList();

        BigDecimal totalAmount = items.stream()
                .map(OrderItem::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setRequestId(requestId);
        order.setTotalAmount(totalAmount);
        order.setStatus(PENDING_PAYMENT);
        order.setRemark(request.remark());
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));
        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            if (requestId != null) {
                Order existing = findOrderByRequestId(userId, requestId);
                if (existing != null) {
                    return toCreateResponse(existing);
                }
            }
            throw ex;
        }
        items.forEach(orderItemMapper::insert);
        orderStatusLogService.record(
                orderNo,
                userId,
                null,
                PENDING_PAYMENT,
                OrderStatusLogService.EVENT_CREATE,
                requestId,
                request.remark()
        );

        Result<Void> lockResult = inventoryClient.lock(new StockLockRequest(
                orderNo,
                items.stream()
                        .map(item -> new StockItemRequest(item.getSkuId(), item.getQuantity()))
                        .toList()
        ));
        assertSuccess(lockResult);
        return toCreateResponse(order);
    }

    @Transactional
    public OrderCreateResponse createSeckill(Long userId,
                                             Long activityId,
                                             Long sessionId,
                                             Long seckillSkuId,
                                             Long skuId,
                                             Integer quantity,
                                             BigDecimal seckillPrice,
                                             String requestId,
                                             String remark) {
        String normalizedRequestId = normalizeRequestId(requestId);
        if (normalizedRequestId == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "seckill requestId is required");
        }
        Order existing = findOrderByRequestId(userId, normalizedRequestId);
        if (existing != null) {
            return toCreateResponse(existing);
        }
        if (quantity == null || quantity < 1 || seckillPrice == null || seckillPrice.signum() <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "invalid seckill order item");
        }

        String orderNo = nextOrderNo();
        OrderItem item = buildOrderItem(orderNo, skuId, quantity, seckillPrice);
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setRequestId(normalizedRequestId);
        order.setTotalAmount(item.getTotalAmount());
        order.setStatus(PENDING_PAYMENT);
        order.setRemark(remark);
        order.setExpireTime(LocalDateTime.now().plusMinutes(payTimeoutMinutes));

        try {
            orderMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            if (normalizedRequestId != null) {
                Order duplicated = findOrderByRequestId(userId, normalizedRequestId);
                if (duplicated != null) {
                    return toCreateResponse(duplicated);
                }
            }
            throw ex;
        }
        orderItemMapper.insert(item);
        orderStatusLogService.record(
                orderNo,
                userId,
                null,
                PENDING_PAYMENT,
                OrderStatusLogService.EVENT_SECKILL_CREATE,
                normalizedRequestId,
                remark
        );

        assertSuccess(inventoryClient.lock(new StockLockRequest(
                orderNo,
                List.of(new StockItemRequest(item.getSkuId(), item.getQuantity()))
        )));
        seckillReservationService.recordReserved(
                normalizedRequestId,
                orderNo,
                userId,
                activityId,
                sessionId,
                seckillSkuId,
                skuId,
                quantity
        );
        return toCreateResponse(order);
    }

    public OrderDetailResponse detailForUser(Long userId, String orderNo) {
        Order order = getOrder(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return toDetail(order, listItems(orderNo));
    }

    public OrderInternalResponse internalDetail(String orderNo) {
        Order order = getOrder(orderNo);
        return new OrderInternalResponse(order.getOrderNo(), order.getUserId(), order.getTotalAmount(), order.getStatus());
    }

    @Transactional
    public OrderCancelResponse cancel(Long userId, String orderNo) {
        Order order = getOrder(orderNo);
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        if (!Integer.valueOf(PENDING_PAYMENT).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "only pending payment order can be canceled");
        }
        assertSuccess(inventoryClient.release(new StockReleaseRequest(orderNo)));
        order.setStatus(CANCELED);
        order.setCancelTime(LocalDateTime.now());
        orderMapper.updateById(order);
        orderStatusLogService.record(
                orderNo,
                userId,
                PENDING_PAYMENT,
                CANCELED,
                OrderStatusLogService.EVENT_USER_CANCEL,
                null,
                "user canceled order"
        );
        seckillReservationService.releaseByOrder(orderNo);
        return new OrderCancelResponse(orderNo, CANCELED);
    }

    @Transactional
    public void markPaid(String orderNo, String payNo) {
        Order order = getOrder(orderNo);
        if (Integer.valueOf(PAID).equals(order.getStatus())) {
            return;
        }
        if (!Integer.valueOf(PENDING_PAYMENT).equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "only pending payment order can be paid");
        }
        if (order.getExpireTime() != null && LocalDateTime.now().isAfter(order.getExpireTime())) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "order is expired");
        }
        assertSuccess(inventoryClient.deduct(new StockDeductRequest(orderNo)));
        LocalDateTime payTime = LocalDateTime.now();
        order.setStatus(PAID);
        order.setPayNo(payNo);
        order.setPayTime(payTime);
        orderMapper.updateById(order);
        orderStatusLogService.record(
                orderNo,
                order.getUserId(),
                PENDING_PAYMENT,
                PAID,
                OrderStatusLogService.EVENT_PAY_SUCCESS,
                payNo,
                "payment success"
        );
        seckillReservationService.markPaidByOrder(orderNo, payTime);
        orderEventPublisher.publishInventoryDeducted(new InventoryDeductedEvent(orderNo, payNo, payTime));
    }

    @Scheduled(fixedDelayString = "${mall.order.expire-scan-delay-ms:30000}")
    public void closeExpiredOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                .eq(Order::getStatus, PENDING_PAYMENT)
                .le(Order::getExpireTime, now)
                .orderByAsc(Order::getExpireTime)
                .last("LIMIT " + expireScanBatchSize));

        for (Order order : orders) {
            try {
                closeExpiredOrder(order, now);
            } catch (RuntimeException ex) {
                log.warn("Close expired order failed, orderNo={}", order.getOrderNo(), ex);
            }
        }
    }

    private void closeExpiredOrder(Order order, LocalDateTime now) {
        assertSuccess(inventoryClient.release(new StockReleaseRequest(order.getOrderNo())));
        int updated = orderMapper.update(null, new UpdateWrapper<Order>()
                .set("status", CANCELED)
                .set("cancel_time", now)
                .eq("id", order.getId())
                .eq("status", PENDING_PAYMENT));
        if (updated != 1) {
            throw new BizException(ErrorCode.ORDER_STATUS_INVALID, "expired order close conflict");
        }
        orderStatusLogService.record(
                order.getOrderNo(),
                order.getUserId(),
                PENDING_PAYMENT,
                CANCELED,
                OrderStatusLogService.EVENT_EXPIRE_CLOSE,
                null,
                "order payment timeout"
        );
        seckillReservationService.releaseByOrder(order.getOrderNo());
    }

    private Map<Long, Integer> aggregateItems(List<OrderItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(ErrorCode.ORDER_ITEM_EMPTY);
        }
        Map<Long, Integer> quantityBySku = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            quantityBySku.merge(item.skuId(), item.quantity(), Integer::sum);
        }
        return quantityBySku;
    }

    private OrderItem buildOrderItem(String orderNo, Long skuId, Integer quantity) {
        Result<ProductSkuResponse> skuResult = productClient.getSku(skuId);
        assertSuccess(skuResult);
        ProductSkuResponse sku = skuResult.getData();
        return buildOrderItem(orderNo, sku, quantity, sku.price());
    }

    private OrderItem buildOrderItem(String orderNo, Long skuId, Integer quantity, BigDecimal unitPrice) {
        Result<ProductSkuResponse> skuResult = productClient.getSku(skuId);
        assertSuccess(skuResult);
        return buildOrderItem(orderNo, skuResult.getData(), quantity, unitPrice);
    }

    private OrderItem buildOrderItem(String orderNo, ProductSkuResponse sku, Integer quantity, BigDecimal unitPrice) {
        BigDecimal totalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));

        OrderItem item = new OrderItem();
        item.setOrderNo(orderNo);
        item.setSkuId(sku.skuId());
        item.setProductId(sku.productId());
        item.setProductName(sku.productName());
        item.setSkuCode(sku.skuCode());
        item.setSpecJson(sku.specJson());
        item.setQuantity(quantity);
        item.setPrice(unitPrice);
        item.setTotalAmount(totalAmount);
        return item;
    }

    private Order getOrder(String orderNo) {
        Order order = orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo));
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private Order findOrderByRequestId(Long userId, String requestId) {
        return orderMapper.selectOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, userId)
                .eq(Order::getRequestId, requestId));
    }

    private OrderCreateResponse toCreateResponse(Order order) {
        return new OrderCreateResponse(order.getOrderNo(), order.getTotalAmount(), order.getStatus());
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        return requestId.trim();
    }

    private List<OrderItem> listItems(String orderNo) {
        return orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo)
                .orderByAsc(OrderItem::getId));
    }

    private OrderDetailResponse toDetail(Order order, List<OrderItem> items) {
        return new OrderDetailResponse(
                order.getOrderNo(),
                order.getUserId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getPayNo(),
                order.getPayTime(),
                order.getCancelTime(),
                order.getExpireTime(),
                order.getRemark(),
                items.stream().map(this::toItemResponse).toList()
        );
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getSkuId(),
                item.getProductId(),
                item.getProductName(),
                item.getSkuCode(),
                item.getSpecJson(),
                item.getQuantity(),
                item.getPrice(),
                item.getTotalAmount()
        );
    }

    private String nextOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_FORMATTER)
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private void assertSuccess(Result<?> result) {
        if (result == null || !result.isSuccess()) {
            String message = result == null ? "remote service call failed" : result.getMessage();
            throw new BizException(ErrorCode.INTERNAL_ERROR, message);
        }
    }
}
