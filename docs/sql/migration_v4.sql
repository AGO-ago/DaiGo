-- =============================================================
-- 代go V4 增量迁移：收货地址地图选点与定位
-- 适用：已有数据的本地/测试/线上库（保留数据）
-- 特性：幂等，可重复执行
-- 执行：mysql -uroot -p zhao_waimai -e "source docs/sql/migration_v4.sql"
-- =============================================================
USE zhao_waimai;

DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
DELIMITER $$
CREATE PROCEDURE `add_column_if_not_exists`(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition VARCHAR(500)
)
BEGIN
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = DATABASE()
                    AND TABLE_NAME = p_table
                    AND COLUMN_NAME = p_column) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- 地图选点得到的详细地址（如「XX大学西门」）
CALL add_column_if_not_exists('user_address', 'address',
    'VARCHAR(200) DEFAULT NULL COMMENT ''地图选点详细地址'' AFTER `office`');
-- 地图选点纬度
CALL add_column_if_not_exists('user_address', 'latitude',
    'DECIMAL(10,7) DEFAULT NULL COMMENT ''纬度'' AFTER `address`');
-- 地图选点经度
CALL add_column_if_not_exists('user_address', 'longitude',
    'DECIMAL(10,7) DEFAULT NULL COMMENT ''经度'' AFTER `latitude`');

DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
