CREATE TABLE IF NOT EXISTS oms_mq_consume_log (
    id BIGINT PRIMARY KEY,
    consumer_group VARCHAR(64) NOT NULL,
    message_key VARCHAR(128) NOT NULL,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0 processing, 1 success, 2 failed',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_oms_mq_consume_group_key (consumer_group, message_key),
    KEY idx_oms_mq_consume_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
