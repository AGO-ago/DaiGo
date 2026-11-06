-- ============================================================
-- v6：商户资质 —— 档口营业执照图片
-- 说明：营业执照属于商户主体资质，挂到 shop 上，入驻后也能持续更新
-- 幂等：字段已存在时自动跳过
-- ============================================================
USE zhao_waimai;

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'zhao_waimai'
      AND TABLE_NAME = 'shop'
      AND COLUMN_NAME = 'license_img'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE `shop` ADD COLUMN `license_img` VARCHAR(500) DEFAULT NULL COMMENT ''营业执照图片 URL'' AFTER `announcement`',
    'SELECT ''shop.license_img 已存在，跳过''');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT `COLUMN_NAME`, `COLUMN_TYPE`, `COLUMN_COMMENT`
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'zhao_waimai' AND TABLE_NAME = 'shop' AND COLUMN_NAME = 'license_img';
