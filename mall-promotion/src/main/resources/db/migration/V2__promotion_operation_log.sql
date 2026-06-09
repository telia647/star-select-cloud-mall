CREATE TABLE IF NOT EXISTS promo_operation_log (
    id BIGINT PRIMARY KEY,
    operator_id BIGINT NOT NULL,
    operator_name VARCHAR(64) NOT NULL,
    role_code VARCHAR(32) NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id BIGINT NULL,
    detail JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_promo_operation_log_operator (operator_id, created_at),
    KEY idx_promo_operation_log_resource (resource_type, resource_id),
    KEY idx_promo_operation_log_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
