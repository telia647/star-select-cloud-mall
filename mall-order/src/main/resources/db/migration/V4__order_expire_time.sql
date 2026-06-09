ALTER TABLE oms_order
    ADD COLUMN expire_time DATETIME NULL AFTER cancel_time,
    ADD KEY idx_oms_order_status_expire_time (status, expire_time);
