ALTER TABLE wms_stock_lock
    ADD UNIQUE KEY uk_wms_stock_lock_order_sku (order_no, sku_id);
