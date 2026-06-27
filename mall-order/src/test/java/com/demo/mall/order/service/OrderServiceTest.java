package com.demo.mall.order.service;

import com.demo.mall.common.api.Result;
import com.demo.mall.common.api.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.demo.mall.order.client.InventoryClient;
import com.demo.mall.order.client.ProductClient;
import com.demo.mall.order.client.dto.ProductSkuResponse;
import com.demo.mall.order.client.dto.StockLockRequest;
import com.demo.mall.order.client.dto.StockReleaseRequest;
import com.demo.mall.order.dto.OrderCreateRequest;
import com.demo.mall.order.dto.OrderCreateResponse;
import com.demo.mall.order.dto.OrderItemRequest;
import com.demo.mall.order.dto.OrderListItemResponse;
import com.demo.mall.order.entity.Order;
import com.demo.mall.order.entity.OrderItem;
import com.demo.mall.order.mapper.OrderItemMapper;
import com.demo.mall.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;

class OrderServiceTest {

    private OrderMapper orderMapper;
    private OrderItemMapper orderItemMapper;
    private ProductClient productClient;
    private InventoryClient inventoryClient;
    private OrderEventPublisher orderEventPublisher;
    private SeckillReservationService seckillReservationService;
    private OrderStatusLogService orderStatusLogService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        productClient = mock(ProductClient.class);
        inventoryClient = mock(InventoryClient.class);
        orderEventPublisher = mock(OrderEventPublisher.class);
        seckillReservationService = mock(SeckillReservationService.class);
        orderStatusLogService = mock(OrderStatusLogService.class);
        orderService = new OrderService(
                orderMapper,
                orderItemMapper,
                productClient,
                inventoryClient,
                orderEventPublisher,
                seckillReservationService,
                orderStatusLogService,
                30,
                20
        );
    }

    @Test
    void createReturnsExistingOrderWhenRequestIdAlreadyExists() {
        Order existing = new Order();
        existing.setOrderNo("O1001");
        existing.setUserId(1L);
        existing.setRequestId("req-1");
        existing.setTotalAmount(new BigDecimal("99.00"));
        existing.setStatus(10);
        when(orderMapper.selectOne(any())).thenReturn(existing);

        OrderCreateResponse response = orderService.create(1L, new OrderCreateRequest(
                List.of(new OrderItemRequest(3001L, 1)),
                "retry",
                "req-1"
        ));

        assertThat(response.orderNo()).isEqualTo("O1001");
        assertThat(response.totalAmount()).isEqualByComparingTo("99.00");
        verify(orderMapper, never()).insert(any(Order.class));
        verify(inventoryClient, never()).lock(any(StockLockRequest.class));
    }

    @Test
    void createPersistsRequestIdAndLocksInventory() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(productClient.getSku(3001L)).thenReturn(Result.success(new ProductSkuResponse(
                3001L,
                2001L,
                "Demo Phone",
                "PHONE-128G",
                "{\"color\":\"black\"}",
                new BigDecimal("1999.00"),
                1
        )));
        when(inventoryClient.lock(any(StockLockRequest.class))).thenReturn(Result.success());

        OrderCreateResponse response = orderService.create(1L, new OrderCreateRequest(
                List.of(new OrderItemRequest(3001L, 2)),
                "checkout",
                "req-2"
        ));

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<StockLockRequest> lockCaptor = ArgumentCaptor.forClass(StockLockRequest.class);
        verify(orderMapper).insert(orderCaptor.capture());
        verify(inventoryClient).lock(lockCaptor.capture());

        Order saved = orderCaptor.getValue();
        assertThat(saved.getRequestId()).isEqualTo("req-2");
        assertThat(saved.getExpireTime()).isAfter(LocalDateTime.now().plusMinutes(20));
        assertThat(response.totalAmount()).isEqualByComparingTo("3998.00");
        assertThat(lockCaptor.getValue().items()).hasSize(1);
        assertThat(lockCaptor.getValue().items().get(0).quantity()).isEqualTo(2);
        verify(orderStatusLogService).record(
                eq(saved.getOrderNo()),
                eq(1L),
                eq(null),
                eq(10),
                eq(OrderStatusLogService.EVENT_CREATE),
                eq("req-2"),
                eq("checkout")
        );
    }

    @Test
    void createSeckillUsesPromotionPriceAndRecordsReservation() {
        when(orderMapper.selectOne(any())).thenReturn(null);
        when(productClient.getSku(3001L)).thenReturn(Result.success(new ProductSkuResponse(
                3001L,
                2001L,
                "Demo Phone",
                "PHONE-128G",
                "{\"color\":\"black\"}",
                new BigDecimal("1999.00"),
                1
        )));
        when(inventoryClient.lock(any(StockLockRequest.class))).thenReturn(Result.success());

        OrderCreateResponse response = orderService.createSeckill(
                1L,
                7001L,
                7101L,
                7201L,
                3001L,
                1,
                new BigDecimal("1599.00"),
                "sk-req-1",
                "seckill"
        );

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<com.demo.mall.order.entity.OrderItem> itemCaptor =
                ArgumentCaptor.forClass(com.demo.mall.order.entity.OrderItem.class);
        verify(orderMapper).insert(orderCaptor.capture());
        verify(orderItemMapper).insert(itemCaptor.capture());

        assertThat(itemCaptor.getValue().getPrice()).isEqualByComparingTo("1599.00");
        assertThat(itemCaptor.getValue().getTotalAmount()).isEqualByComparingTo("1599.00");
        assertThat(response.totalAmount()).isEqualByComparingTo("1599.00");
        verify(orderStatusLogService).record(
                eq(orderCaptor.getValue().getOrderNo()),
                eq(1L),
                eq(null),
                eq(10),
                eq(OrderStatusLogService.EVENT_SECKILL_CREATE),
                eq("sk-req-1"),
                eq("seckill")
        );
        verify(seckillReservationService).recordReserved(
                eq("sk-req-1"),
                eq(orderCaptor.getValue().getOrderNo()),
                eq(1L),
                eq(7001L),
                eq(7101L),
                eq(7201L),
                eq(3001L),
                eq(1)
        );
    }

    @Test
    void listForUserReturnsPagedOrderSummaries() {
        Order order = new Order();
        order.setOrderNo("O2001");
        order.setUserId(1L);
        order.setTotalAmount(new BigDecimal("299.00"));
        order.setStatus(10);
        order.setRemark("checkout");
        Page<Order> page = Page.of(1, 10);
        page.setRecords(List.of(order));
        page.setTotal(1);
        when(orderMapper.selectPage(any(), any())).thenReturn(page);

        OrderItem item = new OrderItem();
        item.setOrderNo("O2001");
        item.setProductName("Demo Phone");
        item.setQuantity(2);
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        PageResult<OrderListItemResponse> result = orderService.listForUser(1L, 1, 10);

        assertThat(result.total()).isEqualTo(1);
        assertThat(result.records()).hasSize(1);
        OrderListItemResponse summary = result.records().get(0);
        assertThat(summary.orderNo()).isEqualTo("O2001");
        assertThat(summary.itemCount()).isEqualTo(2);
        assertThat(summary.firstProductName()).isEqualTo("Demo Phone");
    }

    @Test
    void closeExpiredOrdersReleasesInventoryAndCancelsOrder() {
        Order expired = new Order();
        expired.setId(10L);
        expired.setOrderNo("O1002");
        expired.setStatus(10);
        expired.setExpireTime(LocalDateTime.now().minusMinutes(1));
        when(orderMapper.selectList(any())).thenReturn(List.of(expired));
        when(inventoryClient.release(any(StockReleaseRequest.class))).thenReturn(Result.success());
        when(orderMapper.update(any(), any())).thenReturn(1);

        orderService.closeExpiredOrders();

        verify(inventoryClient).release(any(StockReleaseRequest.class));
        verify(orderMapper).update(any(), any());
        verify(orderStatusLogService).record(
                eq("O1002"),
                eq(null),
                eq(10),
                eq(30),
                eq(OrderStatusLogService.EVENT_EXPIRE_CLOSE),
                eq(null),
                eq("order payment timeout")
        );
        verify(seckillReservationService).releaseByOrder("O1002");
    }
}
