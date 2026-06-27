CREATE TABLE IF NOT EXISTS pms_shop (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(32) NOT NULL,
    logo_url VARCHAR(255) NULL,
    service_tags VARCHAR(255) NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_pms_shop_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE pms_product
    ADD COLUMN shop_id BIGINT NULL AFTER category_id;

CREATE INDEX idx_pms_product_shop_id ON pms_product (shop_id);

INSERT INTO pms_shop (id, name, type, logo_url, service_tags, status)
VALUES
    (5001, '星选自营旗舰店', 'SELF_OPERATED', NULL, '正品保障,次日达,七天无理由', 1),
    (5002, '曜声数码专营店', 'MERCHANT', NULL, '官方授权,极速发货', 1),
    (5003, '沐刻居家生活馆', 'MERCHANT', NULL, '品质家居,破损包退', 1),
    (5004, '光研美妆旗舰店', 'BRAND', NULL, '品牌直营,假一赔十', 1),
    (5005, '跃行运动户外店', 'MERCHANT', NULL, '专业运动,尺码无忧', 1),
    (5006, '城行服饰箱包店', 'MERCHANT', NULL, '通勤精选,包邮包退', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    type = VALUES(type),
    service_tags = VALUES(service_tags),
    status = VALUES(status);

INSERT INTO pms_category (id, parent_id, name, sort, status)
VALUES
    (1001, 0, '精选数码', 10, 1),
    (1002, 0, '电脑办公', 20, 1),
    (1003, 0, '居家生活', 30, 1),
    (1004, 0, '美妆个护', 40, 1),
    (1005, 0, '运动户外', 50, 1),
    (1006, 0, '服饰箱包', 60, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort = VALUES(sort),
    status = VALUES(status);

UPDATE pms_product SET shop_id = 5001 WHERE id IN (2001, 2002) AND shop_id IS NULL;

INSERT INTO pms_product (id, category_id, shop_id, name, subtitle, status)
VALUES
    (2010, 1001, 5002, '曜声降噪头戴耳机', '自适应降噪、40 小时续航，适合通勤和专注办公。', 1),
    (2011, 1003, 5003, '沐刻温控手冲壶', '精准控温、细口稳定水流，让日常咖啡更接近专业出品。', 1),
    (2012, 1004, 5004, '光研玻色因精华', '轻盈乳感质地，主打保湿、提亮和夜间修护。', 1),
    (2013, 1005, 5005, '跃行缓震跑鞋', '轻量回弹中底，兼顾日常慢跑和城市通勤。', 1),
    (2014, 1003, 5003, '巢境香薰空气仪', '微雾扩香、空气状态提醒，适合卧室和办公桌。', 1),
    (2015, 1006, 5006, '城行轻量通勤包', '分层收纳、可放 15 英寸电脑，适合城市移动办公。', 1),
    (2016, 1001, 5002, '脉点迷你智能音箱', '小体积大声场，支持多房间联动和语音助手。', 1),
    (2017, 1006, 5006, '柔层羊毛混纺围巾', '柔软亲肤、轻暖不厚重，是冬季搭配的实用单品。', 1),
    (2018, 1002, 5001, '星选轻薄办公本', '高色域屏幕、长续航和轻薄机身，适合移动办公。', 1),
    (2019, 1004, 5004, '光研氨基酸洁面乳', '温和清洁，适合早晚日常洁面和敏感肌使用。', 1)
ON DUPLICATE KEY UPDATE
    category_id = VALUES(category_id),
    shop_id = VALUES(shop_id),
    name = VALUES(name),
    subtitle = VALUES(subtitle),
    status = VALUES(status);

INSERT INTO pms_sku (id, product_id, sku_code, spec_json, price, status)
VALUES
    (3010, 2010, 'HEADSET-BLACK-STD', JSON_OBJECT('颜色', '曜石黑', '版本', '标准版'), 799.00, 1),
    (3011, 2010, 'HEADSET-SILVER-TRAVEL', JSON_OBJECT('颜色', '月光银', '版本', '旅行套装'), 899.00, 1),
    (3012, 2011, 'KETTLE-GRAY-900ML', JSON_OBJECT('容量', '900ml', '颜色', '岩灰'), 469.00, 1),
    (3013, 2011, 'KETTLE-WHITE-1200ML', JSON_OBJECT('容量', '1.2L', '颜色', '奶白'), 529.00, 1),
    (3014, 2012, 'SERUM-30ML-SINGLE', JSON_OBJECT('规格', '30ml', '套组', '单瓶'), 329.00, 1),
    (3015, 2012, 'SERUM-60ML-DUO', JSON_OBJECT('规格', '60ml', '套组', '双瓶装'), 579.00, 1),
    (3016, 2013, 'RUNNER-BLUE-42', JSON_OBJECT('尺码', '42', '颜色', '雾蓝'), 639.00, 1),
    (3017, 2013, 'RUNNER-BLACK-43', JSON_OBJECT('尺码', '43', '颜色', '炭黑'), 639.00, 1),
    (3018, 2014, 'AROMA-WHITE-CEDAR', JSON_OBJECT('颜色', '暖白', '香氛', '雪松'), 299.00, 1),
    (3019, 2014, 'AROMA-MINT-ORANGE', JSON_OBJECT('颜色', '薄荷绿', '香氛', '橙花'), 319.00, 1),
    (3020, 2015, 'BAG-NAVY-18L', JSON_OBJECT('容量', '18L', '颜色', '深海蓝'), 399.00, 1),
    (3021, 2015, 'BAG-GRAY-24L', JSON_OBJECT('容量', '24L', '颜色', '石墨灰'), 459.00, 1),
    (3022, 2016, 'SPEAKER-WHITE-SINGLE', JSON_OBJECT('颜色', '云白', '套餐', '单只'), 269.00, 1),
    (3023, 2016, 'SPEAKER-WHITE-DUO', JSON_OBJECT('颜色', '云白', '套餐', '双只立体声'), 499.00, 1),
    (3024, 2017, 'SCARF-CAMEL-180', JSON_OBJECT('颜色', '驼色', '尺寸', '180cm'), 219.00, 1),
    (3025, 2017, 'SCARF-GRAY-180', JSON_OBJECT('颜色', '烟灰', '尺寸', '180cm'), 219.00, 1),
    (3026, 2018, 'NOTEBOOK-GRAY-16G', JSON_OBJECT('颜色', '星云灰', '内存', '16G'), 5299.00, 1),
    (3027, 2018, 'NOTEBOOK-SILVER-32G', JSON_OBJECT('颜色', '银色', '内存', '32G'), 6299.00, 1),
    (3028, 2019, 'CLEANSER-120G', JSON_OBJECT('规格', '120g', '套组', '单支'), 119.00, 1),
    (3029, 2019, 'CLEANSER-240G-DUO', JSON_OBJECT('规格', '240g', '套组', '双支'), 199.00, 1)
ON DUPLICATE KEY UPDATE
    product_id = VALUES(product_id),
    spec_json = VALUES(spec_json),
    price = VALUES(price),
    status = VALUES(status);
