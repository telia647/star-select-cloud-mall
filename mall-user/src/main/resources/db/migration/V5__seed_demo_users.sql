INSERT INTO ums_user (id, username, password, phone, status, role_code)
VALUES
    (10001, 'demo', '$2a$10$X3tRdnZBrOqv.K/Y6RJ9O.PLiCh1hcYl7jN6JHoW4ChK3efVB/N36', '13800000000', 1, 'MEMBER'),
    (10002, 'alice', '$2a$10$X3tRdnZBrOqv.K/Y6RJ9O.PLiCh1hcYl7jN6JHoW4ChK3efVB/N36', '13800000001', 1, 'MEMBER'),
    (10003, 'bob', '$2a$10$X3tRdnZBrOqv.K/Y6RJ9O.PLiCh1hcYl7jN6JHoW4ChK3efVB/N36', '13800000002', 1, 'MEMBER'),
    (10004, 'carol', '$2a$10$X3tRdnZBrOqv.K/Y6RJ9O.PLiCh1hcYl7jN6JHoW4ChK3efVB/N36', '13800000003', 1, 'MEMBER'),
    (10005, 'ops', '$2a$10$X3tRdnZBrOqv.K/Y6RJ9O.PLiCh1hcYl7jN6JHoW4ChK3efVB/N36', '13900000001', 1, 'ADMIN')
ON DUPLICATE KEY UPDATE
    phone = VALUES(phone),
    status = VALUES(status),
    role_code = VALUES(role_code);

INSERT INTO ums_user_address (
    id, user_id, receiver_name, phone, province, city, detail_address, default_flag
)
VALUES
    (11001, 10001, '演示用户', '13800000000', '浙江省', '杭州市', '西湖区星选公寓 1 幢 101', 1),
    (11002, 10002, 'Alice', '13800000001', '上海市', '上海市', '浦东新区商城路 88 号', 1),
    (11003, 10003, 'Bob', '13800000002', '广东省', '深圳市', '南山区科技园 9 栋', 1),
    (11004, 10004, 'Carol', '13800000003', '北京市', '北京市', '朝阳区望京 SOHO T1', 1)
ON DUPLICATE KEY UPDATE
    receiver_name = VALUES(receiver_name),
    phone = VALUES(phone),
    province = VALUES(province),
    city = VALUES(city),
    detail_address = VALUES(detail_address),
    default_flag = VALUES(default_flag);
