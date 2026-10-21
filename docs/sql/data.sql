-- =============================================================
-- 本地联调测试数据（仅 dev 环境使用，禁止导入生产）
-- 可重复执行：先清空主数据表
-- =============================================================
USE zhao_waimai;

TRUNCATE TABLE `mall`;
TRUNCATE TABLE `shop`;
TRUNCATE TABLE `dish_category`;
TRUNCATE TABLE `dish`;

-- -------------------------------------------------------------
-- 美食城（青岛黄岛区，带经纬度用于距离计算）
-- -------------------------------------------------------------
INSERT INTO `mall` (`id`, `name`, `address`, `longitude`, `latitude`, `cover_img`, `business_hours`, `status`) VALUES
(1, '橙盟·活力校园MALL', '青岛理工大学(黄岛校区)东门', 120.1878000, 35.9936000,
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=Chinese%20food%20court%20interior%20with%20many%20food%20stalls%20warm%20lighting%20realistic%20photo&image_size=landscape_4_3', '08:00-20:30', 1),
(2, '鲜甜滋味·汉堡·披萨店', '青岛理工大学(黄岛校区)南门', 120.1935000, 35.9902000,
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=burger%20and%20pizza%20restaurant%20storefront%20bright%20modern%20realistic%20photo&image_size=landscape_4_3', '09:00-22:00', 1),
(3, '荷园大排档', '青岛理工大学(黄岛校区)荷园生活区', 120.1830000, 35.9968000,
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=Chinese%20street%20food%20night%20market%20stalls%20warm%20lights%20realistic%20photo&image_size=landscape_4_3', '10:00-21:00', 1),
(4, '泰山主题餐厅', '青岛理工大学(黄岛校区)北区食堂', 120.1901000, 35.9884000,
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=traditional%20Chinese%20restaurant%20dining%20hall%20wooden%20decor%20realistic%20photo&image_size=landscape_4_3', '10:30-20:00', 1);

-- -------------------------------------------------------------
-- 档口
-- -------------------------------------------------------------
INSERT INTO `shop` (`id`, `mall_id`, `name`, `logo`, `phone`, `delivery_fee`, `min_order_amount`, `average_delivery_time`, `business_hours`, `status`) VALUES
(1, 1, '川香快餐',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=Chinese%20fast%20food%20stall%20logo%20red%20orange%20sign%20realistic&image_size=square',
 '13800001001', 2.00, 15.00, 30, '10:00-19:30', 1),
(2, 1, '面面俱到',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=noodle%20restaurant%20logo%20wooden%20style%20realistic&image_size=square',
 '13800001002', 1.50, 12.00, 25, '09:30-20:00', 1),
(3, 1, '老城牛肉饭',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=beef%20rice%20bowl%20restaurant%20logo%20warm%20red%20realistic&image_size=square',
 '13800001003', 2.00, 18.00, 28, '10:00-20:00', 1),
(4, 1, '陈公卤肉',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=braised%20pork%20rice%20shop%20logo%20brown%20realistic&image_size=square',
 '13800001004', 1.00, 15.00, 22, '10:00-19:00', 1),
(5, 2, '伦敦堡',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=burger%20shop%20logo%20yellow%20and%20red%20realistic&image_size=square',
 '13800002001', 3.00, 20.00, 35, '09:00-22:00', 1),
(6, 2, '意面·脆皮',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=italian%20pasta%20restaurant%20logo%20green%20realistic&image_size=square',
 '13800002002', 3.00, 22.00, 38, '09:00-22:00', 1),
(7, 2, '魔力卷饼',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=mexican%20burrito%20wrap%20shop%20logo%20orange%20realistic&image_size=square',
 '13800002003', 2.50, 15.00, 30, '09:00-21:00', 1),
(8, 3, '重庆鸡公煲',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=spicy%20chicken%20pot%20restaurant%20logo%20red%20realistic&image_size=square',
 '13800003001', 2.00, 25.00, 40, '10:00-21:00', 1),
(9, 3, '酸菜鱼粉',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=fish%20noodle%20soup%20shop%20logo%20green%20realistic&image_size=square',
 '13800003002', 2.00, 18.00, 32, '10:00-20:30', 1),
(10, 3, '煎饼果子',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=Chinese%20pancake%20breakfast%20stall%20logo%20yellow%20realistic&image_size=square',
 '13800003003', 1.00, 8.00, 20, '06:30-11:00', 1),
(11, 4, '泰山炒鸡',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=Chinese%20stir%20fried%20chicken%20restaurant%20logo%20realistic&image_size=square',
 '13800004001', 2.50, 28.00, 40, '10:30-20:00', 1),
(12, 4, '大风车拌饭',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=Korean%20bibimbap%20rice%20bowl%20shop%20logo%20realistic&image_size=square',
 '13800004002', 2.00, 16.00, 30, '10:30-20:00', 1);

-- -------------------------------------------------------------
-- 分类
-- -------------------------------------------------------------
INSERT INTO `dish_category` (`id`, `shop_id`, `name`, `sort`) VALUES
(1, 1, '盖饭', 0),
(2, 1, '小炒', 1),
(3, 2, '汤面', 0),
(4, 2, '拌面', 1);

-- -------------------------------------------------------------
-- 菜品
-- -------------------------------------------------------------
INSERT INTO `dish` (`id`, `shop_id`, `category_id`, `name`, `image`, `price`, `original_price`, `description`, `status`, `sort`, `sold_count`) VALUES
(1, 1, 1, '宫保鸡丁盖饭',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=kung%20pao%20chicken%20with%20rice%20bowl%20chinese%20food%20photography&image_size=square',
 18.00, 20.00, '经典川味，花生脆香', 1, 0, 326),
(2, 1, 1, '鱼香肉丝盖饭',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=shredded%20pork%20in%20garlic%20sauce%20rice%20bowl%20chinese%20food&image_size=square',
 17.00, NULL, '酸甜适口，下饭神器', 1, 1, 289),
(3, 1, 2, '麻辣香锅（微辣）',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=spicy%20chinese%20stir%20fry%20hot%20pot%20bowl%20realistic%20food%20photo&image_size=square',
 32.00, 35.00, '自选主料，微辣入门', 1, 0, 158),
(4, 1, 2, '水煮肉片',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=sichuan%20boiled%20pork%20slices%20spicy%20oil%20bowl&image_size=square',
 28.00, NULL, '麻辣鲜香，量足', 1, 1, 132),
(5, 1, 2, '剁椒鱼头（已下架示例）', NULL, 45.00, NULL, '测试下架过滤', 0, 2, 10),
(6, 2, 3, '牛肉拉面',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=beef%20noodle%20soup%20bowl%20chinese%20food%20photography&image_size=square',
 16.00, 18.00, '手工拉面，汤浓味厚', 1, 0, 412),
(7, 2, 3, '肥肠酸辣粉',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=spicy%20sour%20noodle%20soup%20bowl%20chinese&image_size=square',
 14.00, NULL, '酸辣开胃', 1, 1, 367),
(8, 2, 4, '炸酱面',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=zhajiang%20noodles%20beijing%20style%20bowl&image_size=square',
 15.00, NULL, '老北京风味', 1, 0, 205),
(9, 2, 4, '葱油拌面',
 'https://console.enterprise.trae.cn/api/ide/v1/text_to_image?prompt=scallion%20oil%20noodles%20bowl&image_size=square',
 12.00, 13.00, '葱香浓郁', 1, 1, 189);
