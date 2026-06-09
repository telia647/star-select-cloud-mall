CREATE TABLE IF NOT EXISTS promo_activity (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    title VARCHAR(128) NOT NULL,
    description VARCHAR(512) NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_promo_activity_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS promo_session (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    name VARCHAR(64) NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    sort INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_promo_session_activity_id (activity_id),
    KEY idx_promo_session_time (start_time, end_time),
    KEY idx_promo_session_status_sort (status, sort)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS promo_seckill_sku (
    id BIGINT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    session_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    sku_code VARCHAR(64) NOT NULL,
    subtitle VARCHAR(255) NULL,
    original_price DECIMAL(10, 2) NOT NULL,
    seckill_price DECIMAL(10, 2) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    limit_per_user INT NOT NULL DEFAULT 1,
    badge VARCHAR(32) NULL,
    sort INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_promo_seckill_sku_session_sku (session_id, sku_id),
    KEY idx_promo_seckill_sku_session (session_id, status, sort),
    KEY idx_promo_seckill_sku_activity (activity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO promo_activity (id, name, title, description, status)
VALUES
    (7001, 'NovaMall Flash Sale', 'NovaMall Flash Sale', 'Seeded flash-sale activity for local smoke tests and resume demos.', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    title = VALUES(title),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO promo_session (id, activity_id, name, start_time, end_time, status, sort)
VALUES
    (7101, 7001, 'Live Drop', DATE_SUB(NOW(), INTERVAL 1 HOUR), DATE_ADD(NOW(), INTERVAL 2 HOUR), 1, 1),
    (7102, 7001, 'Next Drop', DATE_ADD(NOW(), INTERVAL 3 HOUR), DATE_ADD(NOW(), INTERVAL 5 HOUR), 1, 2),
    (7103, 7001, 'Night Drop', DATE_ADD(NOW(), INTERVAL 6 HOUR), DATE_ADD(NOW(), INTERVAL 8 HOUR), 1, 3)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    status = VALUES(status),
    sort = VALUES(sort);

INSERT INTO promo_seckill_sku (
    id, activity_id, session_id, sku_id, product_id, product_name, sku_code, subtitle,
    original_price, seckill_price, total_stock, available_stock, limit_per_user, badge, sort, status
)
VALUES
    (7201, 7001, 7101, 3001, 2001, 'Mall Demo Phone', 'PHONE-BLACK-128G', 'Live drop limited stock for smoke tests.', 1999.00, 1599.00, 120, 36, 1, 'Live', 1, 1),
    (7202, 7001, 7101, 3002, 2001, 'Mall Demo Phone', 'PHONE-WHITE-256G', 'Large storage model with flash-sale pricing.', 2399.00, 1899.00, 80, 18, 1, 'Limited', 2, 1),
    (7203, 7001, 7102, 3003, 2002, 'Mall Demo Laptop', 'LAPTOP-GRAY-16G', 'Upcoming laptop deal for the next session.', 5999.00, 4999.00, 60, 60, 1, 'Next', 1, 1),
    (7204, 7001, 7103, 3001, 2001, 'Mall Demo Phone', 'PHONE-BLACK-128G', 'Night-session restock with a lower flash-sale price.', 1999.00, 1499.00, 100, 100, 1, 'Night', 1, 1)
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    sku_code = VALUES(sku_code),
    subtitle = VALUES(subtitle),
    original_price = VALUES(original_price),
    seckill_price = VALUES(seckill_price),
    total_stock = VALUES(total_stock),
    available_stock = VALUES(available_stock),
    limit_per_user = VALUES(limit_per_user),
    badge = VALUES(badge),
    sort = VALUES(sort),
    status = VALUES(status);
