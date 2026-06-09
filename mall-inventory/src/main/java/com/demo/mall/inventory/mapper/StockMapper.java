package com.demo.mall.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.mall.inventory.entity.Stock;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface StockMapper extends BaseMapper<Stock> {

    @Select("""
            SELECT id, sku_id, available_stock, locked_stock, created_at, updated_at
            FROM wms_stock
            WHERE sku_id = #{skuId}
            FOR UPDATE
            """)
    Stock selectBySkuIdForUpdate(@Param("skuId") Long skuId);

    @Update("""
            UPDATE wms_stock
            SET available_stock = available_stock - #{quantity},
                locked_stock = locked_stock + #{quantity}
            WHERE sku_id = #{skuId}
              AND available_stock >= #{quantity}
            """)
    int lock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE wms_stock
            SET available_stock = available_stock + #{quantity},
                locked_stock = locked_stock - #{quantity}
            WHERE sku_id = #{skuId}
              AND locked_stock >= #{quantity}
            """)
    int release(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("""
            UPDATE wms_stock
            SET locked_stock = locked_stock - #{quantity}
            WHERE sku_id = #{skuId}
              AND locked_stock >= #{quantity}
            """)
    int deduct(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
