CREATE TABLE IF NOT EXISTS wms_stock_flow (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    sku_id BIGINT NOT NULL,
    operation VARCHAR(32) NOT NULL COMMENT 'LOCK, RELEASE, DEDUCT',
    quantity INT NOT NULL,
    before_available_stock INT NOT NULL,
    after_available_stock INT NOT NULL,
    before_locked_stock INT NOT NULL,
    after_locked_stock INT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_wms_stock_flow_order_no (order_no),
    KEY idx_wms_stock_flow_sku_id (sku_id),
    KEY idx_wms_stock_flow_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
