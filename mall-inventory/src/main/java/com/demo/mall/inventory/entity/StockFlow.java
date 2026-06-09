package com.demo.mall.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.demo.mall.common.model.BaseEntity;

@TableName("wms_stock_flow")
public class StockFlow extends BaseEntity {

    private String orderNo;
    private Long skuId;
    private String operation;
    private Integer quantity;
    private Integer beforeAvailableStock;
    private Integer afterAvailableStock;
    private Integer beforeLockedStock;
    private Integer afterLockedStock;

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getSkuId() {
        return skuId;
    }

    public void setSkuId(Long skuId) {
        this.skuId = skuId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getBeforeAvailableStock() {
        return beforeAvailableStock;
    }

    public void setBeforeAvailableStock(Integer beforeAvailableStock) {
        this.beforeAvailableStock = beforeAvailableStock;
    }

    public Integer getAfterAvailableStock() {
        return afterAvailableStock;
    }

    public void setAfterAvailableStock(Integer afterAvailableStock) {
        this.afterAvailableStock = afterAvailableStock;
    }

    public Integer getBeforeLockedStock() {
        return beforeLockedStock;
    }

    public void setBeforeLockedStock(Integer beforeLockedStock) {
        this.beforeLockedStock = beforeLockedStock;
    }

    public Integer getAfterLockedStock() {
        return afterLockedStock;
    }

    public void setAfterLockedStock(Integer afterLockedStock) {
        this.afterLockedStock = afterLockedStock;
    }
}
