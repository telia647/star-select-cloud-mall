CREATE TABLE IF NOT EXISTS pms_category (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(64) NOT NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pms_category_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pms_product (
    id BIGINT PRIMARY KEY,
    category_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    subtitle VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pms_product_category_id (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pms_sku (
    id BIGINT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    spec_json JSON NULL,
    price DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pms_sku_sku_code (sku_code),
    KEY idx_pms_sku_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO pms_category (id, parent_id, name, sort, status)
VALUES
    (1001, 0, 'Phone', 1, 1),
    (1002, 0, 'Laptop', 2, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), sort = VALUES(sort), status = VALUES(status);

INSERT INTO pms_product (id, category_id, name, subtitle, status)
VALUES
    (2001, 1001, 'Mall Demo Phone', 'Demo product for gateway smoke test', 1),
    (2002, 1002, 'Mall Demo Laptop', 'Demo product for product detail smoke test', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), subtitle = VALUES(subtitle), status = VALUES(status);

INSERT INTO pms_sku (id, product_id, sku_code, spec_json, price, status)
VALUES
    (3001, 2001, 'PHONE-BLACK-128G', JSON_OBJECT('color', 'black', 'storage', '128G'), 1999.00, 1),
    (3002, 2001, 'PHONE-WHITE-256G', JSON_OBJECT('color', 'white', 'storage', '256G'), 2399.00, 1),
    (3003, 2002, 'LAPTOP-GRAY-16G', JSON_OBJECT('color', 'gray', 'memory', '16G'), 5999.00, 1)
ON DUPLICATE KEY UPDATE price = VALUES(price), status = VALUES(status);
