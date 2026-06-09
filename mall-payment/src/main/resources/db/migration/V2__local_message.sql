CREATE TABLE IF NOT EXISTS pay_local_message (
    id BIGINT PRIMARY KEY,
    message_key VARCHAR(128) NOT NULL,
    binding_name VARCHAR(64) NOT NULL,
    topic VARCHAR(128) NOT NULL,
    tag VARCHAR(64) NULL,
    payload JSON NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 pending, 1 sent, 2 failed',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(512) NULL,
    sent_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_pay_local_message_key (message_key),
    KEY idx_pay_local_message_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
