-- =============================================================
-- 本地联调测试数据（仅 dev 环境使用，禁止导入生产）
-- =============================================================
USE zhao_waimai;

-- 美食城
INSERT INTO `mall` (`name`, `address`, `business_hours`, `status`) VALUES
('中关村食宝广场', '北京市海淀区中关村大街 1 号', '08:00-20:30', 1);

-- 档口
INSERT INTO `shop` (`mall_id`, `name`, `phone`, `delivery_fee`, `min_order_amount`, `average_delivery_time`, `business_hours`, `status`) VALUES
(1, '川香快餐', '13800001001', 2.00, 15.00, 30, '10:00-19:30', 1),
(1, '面面俱到', '13800001002', 1.50, 12.00, 25, '09:30-20:00', 1);

-- 分类
INSERT INTO `dish_category` (`shop_id`, `name`, `sort`) VALUES
(1, '盖饭', 0),
(1, '小炒', 1),
(2, '汤面', 0),
(2, '拌面', 1);

-- 菜品（川香快餐）
INSERT INTO `dish` (`shop_id`, `category_id`, `name`, `price`, `original_price`, `description`, `status`, `sort`, `sold_count`) VALUES
(1, 1, '宫保鸡丁盖饭', 18.00, 20.00, '经典川味，花生脆香', 1, 0, 326),
(1, 1, '鱼香肉丝盖饭', 17.00, NULL, '酸甜适口，下饭神器', 1, 1, 289),
(1, 2, '麻辣香锅（微辣）', 32.00, 35.00, '自选主料，微辣入门', 1, 0, 158),
(1, 2, '水煮肉片', 28.00, NULL, '麻辣鲜香，量足', 1, 1, 132),
(1, 2, '剁椒鱼头（已下架示例）', 45.00, NULL, '测试下架过滤', 0, 2, 10);

-- 菜品（面面俱到）
INSERT INTO `dish` (`shop_id`, `category_id`, `name`, `price`, `original_price`, `description`, `status`, `sort`, `sold_count`) VALUES
(2, 3, '牛肉拉面', 16.00, 18.00, '手工拉面，汤浓味厚', 1, 0, 412),
(2, 3, '肥肠酸辣粉', 14.00, NULL, '酸辣开胃', 1, 1, 367),
(2, 4, '炸酱面', 15.00, NULL, '老北京风味', 1, 0, 205),
(2, 4, '葱油拌面', 12.00, 13.00, '葱香浓郁', 1, 1, 189);
