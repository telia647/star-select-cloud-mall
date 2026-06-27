INSERT INTO promo_activity (id, name, title, description, status)
VALUES
    (7002, '星选年中好物秒杀', '星选年中好物秒杀', '覆盖数码、居家、美妆、运动和通勤好物的演示秒杀活动。', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    title = VALUES(title),
    description = VALUES(description),
    status = VALUES(status);

INSERT INTO promo_session (id, activity_id, name, start_time, end_time, status, sort)
VALUES
    (7111, 7002, '10:00 早场', DATE_SUB(NOW(), INTERVAL 4 HOUR), DATE_SUB(NOW(), INTERVAL 2 HOUR), 1, 1),
    (7112, 7002, '14:00 午场', DATE_SUB(NOW(), INTERVAL 30 MINUTE), DATE_ADD(NOW(), INTERVAL 90 MINUTE), 1, 2),
    (7113, 7002, '20:00 晚场', DATE_ADD(NOW(), INTERVAL 4 HOUR), DATE_ADD(NOW(), INTERVAL 6 HOUR), 1, 3)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    start_time = VALUES(start_time),
    end_time = VALUES(end_time),
    status = VALUES(status),
    sort = VALUES(sort);

INSERT INTO promo_seckill_sku (
    id, activity_id, session_id, sku_id, product_id, product_name, sku_code, subtitle,
    original_price, seckill_price, total_stock, available_stock, limit_per_user, badge, sort, status
)
VALUES
    (7211, 7002, 7111, 3018, 2014, '巢境香薰空气仪', 'AROMA-WHITE-CEDAR', '早场售罄样例，用于展示已结束状态。', 299.00, 229.00, 80, 0, 1, '已抢光', 1, 1),
    (7212, 7002, 7111, 3024, 2017, '柔层羊毛混纺围巾', 'SCARF-CAMEL-180', '早场售罄样例，展示库存归零。', 219.00, 159.00, 120, 0, 1, '售罄', 2, 1),
    (7213, 7002, 7112, 3010, 2010, '曜声降噪头戴耳机', 'HEADSET-BLACK-STD', '午场爆款，限量开抢。', 799.00, 649.00, 150, 46, 1, '正在抢', 1, 1),
    (7214, 7002, 7112, 3016, 2013, '跃行缓震跑鞋', 'RUNNER-BLUE-42', '运动户外热卖，尺码有限。', 639.00, 489.00, 90, 22, 1, '运动热卖', 2, 1),
    (7215, 7002, 7112, 3020, 2015, '城行轻量通勤包', 'BAG-NAVY-18L', '通勤背包限时直降。', 399.00, 319.00, 80, 30, 1, '通勤精选', 3, 1),
    (7216, 7002, 7113, 3022, 2016, '脉点迷你智能音箱', 'SPEAKER-WHITE-SINGLE', '晚场预告，小身材大声场。', 269.00, 219.00, 100, 100, 1, '即将开始', 1, 1),
    (7217, 7002, 7113, 3014, 2012, '光研玻色因精华', 'SERUM-30ML-SINGLE', '晚场美妆单品预告。', 329.00, 259.00, 100, 100, 1, '预约提醒', 2, 1),
    (7218, 7002, 7113, 3026, 2018, '星选轻薄办公本', 'NOTEBOOK-GRAY-16G', '晚场大额券爆款。', 5299.00, 4899.00, 40, 40, 1, '晚场大牌', 3, 1)
ON DUPLICATE KEY UPDATE
    product_name = VALUES(product_name),
    sku_code = VALUES(sku_code),
    subtitle = VALUES(subtitle),
    original_price = VALUES(original_price),
    seckill_price = VALUES(seckill_price),
    total_stock = VALUES(total_stock),
    available_stock = VALUES(available_stock),
    limit_per_user = VALUES(limit_per_user),
    badge = VALUES(badge),
    sort = VALUES(sort),
    status = VALUES(status);
