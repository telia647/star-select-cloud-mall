CREATE TABLE IF NOT EXISTS ums_member_coupon (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    coupon_name VARCHAR(64) NOT NULL,
    coupon_type VARCHAR(32) NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    threshold_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 unused, 2 used, 3 expired',
    valid_from DATETIME NOT NULL,
    valid_to DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ums_member_coupon_user_status (user_id, status),
    KEY idx_ums_member_coupon_valid_to (valid_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
