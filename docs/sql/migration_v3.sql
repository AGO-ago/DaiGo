-- =============================================================
-- 代go V3 增量迁移：商家网页端登录密码
-- 适用：已有数据的本地/测试/线上库（保留数据）
-- 特性：幂等，可重复执行
-- 执行：mysql -uroot -p zhao_waimai -e "source docs/sql/migration_v3.sql"
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

-- 商家网页端登录密码（未设置时为空）
CALL add_column_if_not_exists('user', 'password',
    'VARCHAR(128) DEFAULT NULL COMMENT ''网页端登录密码（PBKDF2）'' AFTER `phone`');
CALL add_column_if_not_exists('user', 'salt',
    'VARCHAR(64) DEFAULT NULL COMMENT ''密码盐（Base64）'' AFTER `password`');

DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
