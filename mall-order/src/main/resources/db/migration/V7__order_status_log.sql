CREATE TABLE IF NOT EXISTS oms_order_status_log (
    id BIGINT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    from_status TINYINT NULL,
    to_status TINYINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    biz_no VARCHAR(64) NULL,
    remark VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_oms_order_status_log_order_no (order_no),
    KEY idx_oms_order_status_log_user_id (user_id),
    KEY idx_oms_order_status_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
