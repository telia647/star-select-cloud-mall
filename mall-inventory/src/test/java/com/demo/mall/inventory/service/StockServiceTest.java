package com.demo.mall.inventory.service;

import com.demo.mall.inventory.dto.StockItemRequest;
import com.demo.mall.inventory.dto.StockLockRequest;
import com.demo.mall.inventory.entity.Stock;
import com.demo.mall.inventory.entity.StockLock;
import com.demo.mall.inventory.mapper.StockLockMapper;
import com.demo.mall.inventory.mapper.StockMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockServiceTest {

    private StockMapper stockMapper;
    private StockLockMapper stockLockMapper;
    private StockFlowService stockFlowService;
    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockMapper = mock(StockMapper.class);
        stockLockMapper = mock(StockLockMapper.class);
        stockFlowService = mock(StockFlowService.class);
        stockService = new StockService(stockMapper, stockLockMapper, stockFlowService);
    }

    @Test
    void lockIsIdempotentWhenOrderAlreadyHasLocks() {
        when(stockLockMapper.selectList(any())).thenReturn(List.of(new StockLock()));

        stockService.lock(new StockLockRequest("O1001", List.of(new StockItemRequest(3001L, 1))));

        verify(stockMapper, never()).lock(3001L, 1);
        verify(stockLockMapper, never()).insert(any(StockLock.class));
    }

    @Test
    void lockRecordsStockFlow() {
        Stock before = stock(10, 0);
        Stock after = stock(8, 2);
        when(stockLockMapper.selectList(any())).thenReturn(List.of());
        when(stockMapper.selectBySkuIdForUpdate(3001L)).thenReturn(before, after);
        when(stockMapper.lock(3001L, 2)).thenReturn(1);

        stockService.lock(new StockLockRequest("O1001", List.of(new StockItemRequest(3001L, 2))));

        verify(stockMapper).lock(3001L, 2);
        verify(stockLockMapper).insert(any(StockLock.class));
        verify(stockFlowService).record("O1001", 3001L, "LOCK", 2, before, after);
    }

    @Test
    void releaseUnlocksLockedStock() {
        Stock before = stock(10, 2);
        Stock after = stock(12, 0);
        StockLock lock = new StockLock();
        lock.setSkuId(3001L);
        lock.setQuantity(2);
        lock.setStatus(1);
        when(stockLockMapper.selectList(any())).thenReturn(List.of(lock));
        when(stockMapper.selectBySkuIdForUpdate(3001L)).thenReturn(before, after);
        when(stockMapper.release(3001L, 2)).thenReturn(1);

        stockService.release("O1001");

        verify(stockMapper).release(3001L, 2);
        verify(stockLockMapper).updateById(lock);
        verify(stockFlowService).record("O1001", 3001L, "RELEASE", 2, before, after);
    }

    @Test
    void deductIsIdempotentWhenAlreadyDeducted() {
        when(stockLockMapper.selectList(any())).thenReturn(List.of());
        when(stockLockMapper.selectCount(any())).thenReturn(1L);

        stockService.deduct("O1001");

        verify(stockMapper, never()).deduct(3001L, 1);
    }

    private Stock stock(int availableStock, int lockedStock) {
        Stock stock = new Stock();
        stock.setSkuId(3001L);
        stock.setAvailableStock(availableStock);
        stock.setLockedStock(lockedStock);
        return stock;
    }
}
