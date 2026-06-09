CREATE TABLE IF NOT EXISTS wms_stock_lock (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 locked, 2 released, 3 deducted',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_wms_stock_lock_order_no (order_no),
    KEY idx_wms_stock_lock_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
