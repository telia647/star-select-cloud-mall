ALTER TABLE ums_user
    ADD COLUMN role_code VARCHAR(32) NOT NULL DEFAULT 'MEMBER' AFTER status;

CREATE INDEX idx_ums_user_role_code ON ums_user (role_code);
