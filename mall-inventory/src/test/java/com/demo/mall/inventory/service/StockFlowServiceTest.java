package com.demo.mall.inventory.service;

import com.demo.mall.inventory.entity.Stock;
import com.demo.mall.inventory.entity.StockFlow;
import com.demo.mall.inventory.mapper.StockFlowMapper;
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

class StockFlowServiceTest {

    private StockFlowMapper stockFlowMapper;
    private StockFlowService service;

    @BeforeEach
    void setUp() {
        stockFlowMapper = mock(StockFlowMapper.class);
        service = new StockFlowService(stockFlowMapper);
    }

    @Test
    void recordPersistsSnapshot() {
        service.record("O1001", 3001L, "LOCK", 2, stock(10, 0), stock(8, 2));

        ArgumentCaptor<StockFlow> captor = ArgumentCaptor.forClass(StockFlow.class);
        verify(stockFlowMapper).insert(captor.capture());
        assertThat(captor.getValue().getBeforeAvailableStock()).isEqualTo(10);
        assertThat(captor.getValue().getAfterLockedStock()).isEqualTo(2);
    }

    @Test
    void listByOrderNoMapsResponses() {
        StockFlow flow = new StockFlow();
        flow.setOrderNo("O1001");
        flow.setSkuId(3001L);
        flow.setOperation("LOCK");
        flow.setQuantity(2);
        flow.setBeforeAvailableStock(10);
        flow.setAfterAvailableStock(8);
        flow.setBeforeLockedStock(0);
        flow.setAfterLockedStock(2);
        flow.setCreatedAt(LocalDateTime.now());
        when(stockFlowMapper.selectList(any())).thenReturn(List.of(flow));

        var responses = service.listByOrderNo("O1001");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).operation()).isEqualTo("LOCK");
        assertThat(responses.get(0).afterLockedStock()).isEqualTo(2);
    }

    private Stock stock(int availableStock, int lockedStock) {
        Stock stock = new Stock();
        stock.setAvailableStock(availableStock);
        stock.setLockedStock(lockedStock);
        return stock;
    }
}
