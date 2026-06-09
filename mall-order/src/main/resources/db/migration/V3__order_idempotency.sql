ALTER TABLE oms_order
    ADD COLUMN request_id VARCHAR(64) NULL AFTER user_id,
    ADD UNIQUE KEY uk_oms_order_user_request (user_id, request_id);
