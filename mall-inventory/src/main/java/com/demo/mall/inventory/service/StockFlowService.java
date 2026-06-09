package com.demo.mall.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.mall.common.error.BizException;
import com.demo.mall.common.error.ErrorCode;
import com.demo.mall.inventory.dto.StockFlowResponse;
import com.demo.mall.inventory.entity.Stock;
import com.demo.mall.inventory.entity.StockFlow;
import com.demo.mall.inventory.mapper.StockFlowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockFlowService {

    private final StockFlowMapper stockFlowMapper;

    public StockFlowService(StockFlowMapper stockFlowMapper) {
        this.stockFlowMapper = stockFlowMapper;
    }

    public void record(String orderNo, Long skuId, String operation, Integer quantity, Stock before, Stock after) {
        StockFlow flow = new StockFlow();
        flow.setOrderNo(orderNo);
        flow.setSkuId(skuId);
        flow.setOperation(operation);
        flow.setQuantity(quantity);
        flow.setBeforeAvailableStock(before.getAvailableStock());
        flow.setAfterAvailableStock(after.getAvailableStock());
        flow.setBeforeLockedStock(before.getLockedStock());
        flow.setAfterLockedStock(after.getLockedStock());
        stockFlowMapper.insert(flow);
    }

    public List<StockFlowResponse> listByOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "orderNo is required");
        }
        return stockFlowMapper.selectList(new LambdaQueryWrapper<StockFlow>()
                        .eq(StockFlow::getOrderNo, orderNo.trim())
                        .orderByAsc(StockFlow::getCreatedAt)
                        .orderByAsc(StockFlow::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private StockFlowResponse toResponse(StockFlow flow) {
        return new StockFlowResponse(
                flow.getOrderNo(),
                flow.getSkuId(),
                flow.getOperation(),
                flow.getQuantity(),
                flow.getBeforeAvailableStock(),
                flow.getAfterAvailableStock(),
                flow.getBeforeLockedStock(),
                flow.getAfterLockedStock(),
                flow.getCreatedAt()
        );
    }
}
