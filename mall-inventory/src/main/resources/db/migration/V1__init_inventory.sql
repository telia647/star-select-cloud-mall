CREATE TABLE IF NOT EXISTS wms_stock (
    id BIGINT PRIMARY KEY,
    sku_id BIGINT NOT NULL,
    available_stock INT NOT NULL DEFAULT 0,
    locked_stock INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_wms_stock_sku_id (sku_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO wms_stock (id, sku_id, available_stock, locked_stock)
VALUES
    (4001, 3001, 100, 0),
    (4002, 3002, 80, 0),
    (4003, 3003, 50, 0)
ON DUPLICATE KEY UPDATE available_stock = VALUES(available_stock), locked_stock = VALUES(locked_stock);
