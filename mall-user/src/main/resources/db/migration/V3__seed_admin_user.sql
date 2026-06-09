INSERT INTO ums_user (id, username, password, phone, status, role_code)
VALUES
    (10000, 'admin', '$2a$10$X3tRdnZBrOqv.K/Y6RJ9O.PLiCh1hcYl7jN6JHoW4ChK3efVB/N36', '13900000000', 1, 'ADMIN')
ON DUPLICATE KEY UPDATE
    role_code = VALUES(role_code),
    status = VALUES(status);
