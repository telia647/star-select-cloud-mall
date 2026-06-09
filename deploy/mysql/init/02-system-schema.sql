USE mall_system;

CREATE TABLE IF NOT EXISTS sys_operate_log (
    id BIGINT PRIMARY KEY,
    service_name VARCHAR(64) NOT NULL,
    trace_id VARCHAR(64) NULL,
    operate_type VARCHAR(64) NOT NULL,
    content VARCHAR(1024) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sys_operate_log_trace_id (trace_id),
    KEY idx_sys_operate_log_service_name (service_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
