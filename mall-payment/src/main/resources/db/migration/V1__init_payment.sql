CREATE TABLE IF NOT EXISTS pay_payment_order (
    id BIGINT PRIMARY KEY,
    pay_no VARCHAR(64) NOT NULL,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1 success',
    pay_channel VARCHAR(32) NOT NULL,
    paid_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_payment_order_pay_no (pay_no),
    UNIQUE KEY uk_pay_payment_order_order_no_success (order_no, status),
    KEY idx_pay_payment_order_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
