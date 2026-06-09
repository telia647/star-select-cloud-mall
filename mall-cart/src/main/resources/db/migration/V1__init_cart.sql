CREATE TABLE IF NOT EXISTS oms_cart_item (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_json JSON NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_oms_cart_item_user_sku (user_id, sku_id),
    KEY idx_oms_cart_item_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
