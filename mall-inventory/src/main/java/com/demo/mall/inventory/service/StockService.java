package com.demo.mall.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.inventory.dto.StockItemRequest;
import com.demo.mall.inventory.dto.StockLockRequest;
import com.demo.mall.inventory.entity.Stock;
import com.demo.mall.inventory.entity.StockLock;
import com.demo.mall.inventory.mapper.StockLockMapper;
import com.demo.mall.inventory.mapper.StockMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class StockService {

    private static final int LOCKED = 1;
    private static final int RELEASED = 2;
    private static final int DEDUCTED = 3;
    private static final String OPERATION_LOCK = "LOCK";
    private static final String OPERATION_RELEASE = "RELEASE";
    private static final String OPERATION_DEDUCT = "DEDUCT";

    private final StockMapper stockMapper;
    private final StockLockMapper stockLockMapper;
    private final StockFlowService stockFlowService;

    public StockService(StockMapper stockMapper, StockLockMapper stockLockMapper, StockFlowService stockFlowService) {
        this.stockMapper = stockMapper;
        this.stockLockMapper = stockLockMapper;
        this.stockFlowService = stockFlowService;
    }

    @Transactional
    public void lock(StockLockRequest request) {
        List<StockLock> existingLocks = stockLockMapper.selectList(new LambdaQueryWrapper<StockLock>()
                .eq(StockLock::getOrderNo, request.orderNo()));
        if (!existingLocks.isEmpty()) {
            return;
        }

        List<StockItemRequest> items = normalizeItems(request.items());
        for (StockItemRequest item : items) {
            Stock before = requireStock(item.skuId());
            int updated = stockMapper.lock(item.skuId(), item.quantity());
            if (updated != 1) {
                throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "sku " + item.skuId() + " stock is not enough");
            }
            Stock after = requireStock(item.skuId());
            StockLock lock = new StockLock();
            lock.setOrderNo(request.orderNo());
            lock.setSkuId(item.skuId());
            lock.setQuantity(item.quantity());
            lock.setStatus(LOCKED);
            stockLockMapper.insert(lock);
            stockFlowService.record(request.orderNo(), item.skuId(), OPERATION_LOCK, item.quantity(), before, after);
        }
    }

    @Transactional
    public void release(String orderNo) {
        List<StockLock> locks = stockLockMapper.selectList(new LambdaQueryWrapper<StockLock>()
                .eq(StockLock::getOrderNo, orderNo)
                .eq(StockLock::getStatus, LOCKED))
                .stream()
                .sorted((left, right) -> left.getSkuId().compareTo(right.getSkuId()))
                .toList();
        for (StockLock lock : locks) {
            Stock before = requireStock(lock.getSkuId());
            int updated = stockMapper.release(lock.getSkuId(), lock.getQuantity());
            if (updated != 1) {
                throw new BizException(ErrorCode.STOCK_LOCK_NOT_FOUND);
            }
            Stock after = requireStock(lock.getSkuId());
            lock.setStatus(RELEASED);
            stockLockMapper.updateById(lock);
            stockFlowService.record(orderNo, lock.getSkuId(), OPERATION_RELEASE, lock.getQuantity(), before, after);
        }
    }

    @Transactional
    public void deduct(String orderNo) {
        List<StockLock> locks = stockLockMapper.selectList(new LambdaQueryWrapper<StockLock>()
                .eq(StockLock::getOrderNo, orderNo)
                .eq(StockLock::getStatus, LOCKED))
                .stream()
                .sorted((left, right) -> left.getSkuId().compareTo(right.getSkuId()))
                .toList();
        if (locks.isEmpty()) {
            boolean alreadyDeducted = stockLockMapper.selectCount(new LambdaQueryWrapper<StockLock>()
                    .eq(StockLock::getOrderNo, orderNo)
                    .eq(StockLock::getStatus, DEDUCTED)) > 0;
            if (alreadyDeducted) {
                return;
            }
            throw new BizException(ErrorCode.STOCK_LOCK_NOT_FOUND);
        }

        for (StockLock lock : locks) {
            Stock before = requireStock(lock.getSkuId());
            int updated = stockMapper.deduct(lock.getSkuId(), lock.getQuantity());
            if (updated != 1) {
                throw new BizException(ErrorCode.STOCK_LOCK_NOT_FOUND);
            }
            Stock after = requireStock(lock.getSkuId());
            lock.setStatus(DEDUCTED);
            stockLockMapper.updateById(lock);
            stockFlowService.record(orderNo, lock.getSkuId(), OPERATION_DEDUCT, lock.getQuantity(), before, after);
        }
    }

    private Stock requireStock(Long skuId) {
        Stock stock = stockMapper.selectBySkuIdForUpdate(skuId);
        if (stock == null) {
            throw new BizException(ErrorCode.STOCK_NOT_FOUND, "sku " + skuId + " stock not found");
        }
        return stock;
    }

    private List<StockItemRequest> normalizeItems(List<StockItemRequest> items) {
        return items.stream()
                .collect(Collectors.groupingBy(StockItemRequest::skuId, TreeMap::new,
                        Collectors.summingInt(StockItemRequest::quantity)))
                .entrySet()
                .stream()
                .map(entry -> new StockItemRequest(entry.getKey(), entry.getValue()))
                .toList();
    }
}
