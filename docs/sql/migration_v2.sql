-- =============================================================
-- 美食城外卖点餐平台 V2 增量迁移脚本
-- 适用：已有数据的本地/测试库（保留数据）
-- 特性：幂等，可重复执行；禁止用 data.sql 重建库
-- 执行：mysql -uroot -p zhao_waimai < docs/sql/migration_v2.sql
-- =============================================================
USE zhao_waimai;

-- -------------------------------------------------------------
-- 0. 通用工具：列不存在才新增（MySQL 8 无 ADD COLUMN IF NOT EXISTS）
-- -------------------------------------------------------------
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

-- -------------------------------------------------------------
-- 1. 新表
-- -------------------------------------------------------------

-- 1.1 平台管理员
CREATE TABLE IF NOT EXISTS `admin` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`        VARCHAR(32)  NOT NULL COMMENT '登录账号',
  `password`        VARCHAR(200) NOT NULL COMMENT '密码哈希（PBKDF2WithHmacSHA256，Base64）',
  `salt`            VARCHAR(64)  NOT NULL COMMENT '密码盐（Base64）',
  `real_name`       VARCHAR(32)           DEFAULT NULL COMMENT '姓名',
  `role`            TINYINT      NOT NULL DEFAULT 2 COMMENT '角色：1 超管 2 运营',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0 禁用 1 正常',
  `last_login_time` DATETIME              DEFAULT NULL COMMENT '最后登录时间',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE = InnoDB COMMENT = '平台管理员';

-- 1.2 商家入驻申请
CREATE TABLE IF NOT EXISTS `merchant_apply` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`         BIGINT       NOT NULL COMMENT '申请账号 id',
  `shop_name`       VARCHAR(64)  NOT NULL COMMENT '档口名称',
  `mall_id`         BIGINT                DEFAULT NULL COMMENT '所属美食城 id',
  `contact`         VARCHAR(32)  NOT NULL COMMENT '联系人',
  `phone`           VARCHAR(20)  NOT NULL COMMENT '联系电话',
  `license_img`     VARCHAR(500)          DEFAULT NULL COMMENT '营业执照图 URL',
  `business_hours`  VARCHAR(64)           DEFAULT NULL COMMENT '营业时间',
  `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态：0 待审 1 通过 2 驳回',
  `reject_reason`   VARCHAR(200)          DEFAULT NULL COMMENT '驳回原因',
  `audit_admin_id`  BIGINT                DEFAULT NULL COMMENT '审核管理员 id',
  `audit_time`      DATETIME              DEFAULT NULL COMMENT '审核时间',
  `shop_id`         BIGINT                DEFAULT NULL COMMENT '审核通过后绑定的档口 id',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '商家入驻申请';

-- 1.3 退款单
CREATE TABLE IF NOT EXISTS `order_refund` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `refund_no`        VARCHAR(32)   NOT NULL COMMENT '退款单号（唯一）',
  `order_id`         BIGINT        NOT NULL COMMENT '子订单 id',
  `payment_id`       BIGINT                 DEFAULT NULL COMMENT '支付单 id',
  `user_id`          BIGINT        NOT NULL COMMENT '申请用户 id',
  `shop_id`          BIGINT        NOT NULL COMMENT '档口 id',
  `amount`           DECIMAL(10,2) NOT NULL COMMENT '退款金额',
  `reason`           VARCHAR(200)  NOT NULL COMMENT '退款原因',
  `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 申请中 1 已同意 2 已拒绝 3 已退款',
  `reject_reason`    VARCHAR(200)           DEFAULT NULL COMMENT '拒绝原因',
  `handle_admin_id`  BIGINT                 DEFAULT NULL COMMENT '处理管理员 id',
  `handle_time`      DATETIME               DEFAULT NULL COMMENT '处理时间',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_refund_no` (`refund_no`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_shop_id_status` (`shop_id`, `status`)
) ENGINE = InnoDB COMMENT = '退款单';

-- 1.4 结算单
CREATE TABLE IF NOT EXISTS `settlement` (
  `id`                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `settlement_no`     VARCHAR(32)   NOT NULL COMMENT '结算单号（唯一）',
  `shop_id`           BIGINT        NOT NULL COMMENT '档口 id',
  `period_start`      DATE          NOT NULL COMMENT '结算周期开始',
  `period_end`        DATE          NOT NULL COMMENT '结算周期结束',
  `order_count`       INT           NOT NULL DEFAULT 0 COMMENT '订单数',
  `goods_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '商品金额',
  `delivery_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '配送费',
  `discount_amount`   DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  `commission_rate`   DECIMAL(5,4)  NOT NULL DEFAULT 0 COMMENT '抽佣比例',
  `commission_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '抽佣金额',
  `refund_amount`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '退款金额',
  `net_amount`        DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '应结金额',
  `status`            TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待结算 1 已结算',
  `settle_time`       DATETIME               DEFAULT NULL COMMENT '打款/结算时间',
  `create_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_settlement_no` (`settlement_no`),
  KEY `idx_shop_period` (`shop_id`, `period_start`, `period_end`)
) ENGINE = InnoDB COMMENT = '结算单';

-- 1.5 平台配置
CREATE TABLE IF NOT EXISTS `platform_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`   VARCHAR(64)  NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
  `remark`       VARCHAR(200)          DEFAULT NULL COMMENT '说明',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE = InnoDB COMMENT = '平台配置';

-- 1.6 秒杀活动
CREATE TABLE IF NOT EXISTS `seckill_activity` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id`        BIGINT        NOT NULL COMMENT '菜品 id',
  `shop_id`        BIGINT        NOT NULL COMMENT '档口 id',
  `seckill_price`  DECIMAL(10,2) NOT NULL COMMENT '秒杀价',
  `start_time`     DATETIME      NOT NULL COMMENT '开始时间',
  `end_time`       DATETIME      NOT NULL COMMENT '结束时间',
  `stock`          INT           NOT NULL DEFAULT 0 COMMENT '活动库存',
  `sold_count`     INT           NOT NULL DEFAULT 0 COMMENT '已售数量',
  `limit_per_user` INT           NOT NULL DEFAULT 1 COMMENT '每人限购份数',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0 下线 1 上线',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_dish_time` (`dish_id`, `start_time`, `end_time`),
  KEY `idx_status_time` (`status`, `start_time`, `end_time`)
) ENGINE = InnoDB COMMENT = '秒杀活动';

-- -------------------------------------------------------------
-- 2. 既有表变更
-- -------------------------------------------------------------

-- 2.1 档口：公告、自动接单、抽佣比例
CALL add_column_if_not_exists('shop', 'announcement', 'VARCHAR(200) DEFAULT NULL COMMENT ''档口公告''');
CALL add_column_if_not_exists('shop', 'auto_accept', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''自动接单：0 否 1 是''');
CALL add_column_if_not_exists('shop', 'commission_rate', 'DECIMAL(5,4) DEFAULT NULL COMMENT ''抽佣比例，空取平台默认''');

-- 2.2 订单项：规格快照
CALL add_column_if_not_exists('order_item', 'spec_id', 'BIGINT DEFAULT NULL COMMENT ''规格 id''');
CALL add_column_if_not_exists('order_item', 'spec_price', 'DECIMAL(10,2) DEFAULT NULL COMMENT ''规格加价快照''');

-- 2.3 评价：商家回复、平台回复、隐藏
CALL add_column_if_not_exists('order_comment', 'reply_content', 'VARCHAR(200) DEFAULT NULL COMMENT ''商家回复''');
CALL add_column_if_not_exists('order_comment', 'reply_time', 'DATETIME DEFAULT NULL COMMENT ''商家回复时间''');
CALL add_column_if_not_exists('order_comment', 'admin_reply', 'VARCHAR(200) DEFAULT NULL COMMENT ''平台回复''');
CALL add_column_if_not_exists('order_comment', 'hidden', 'TINYINT NOT NULL DEFAULT 0 COMMENT ''是否隐藏：0 否 1 是''');

-- 2.4 菜品规格：排序、启用状态
CALL add_column_if_not_exists('dish_spec', 'sort', 'INT NOT NULL DEFAULT 0 COMMENT ''排序值，越小越靠前''');
CALL add_column_if_not_exists('dish_spec', 'status', 'TINYINT NOT NULL DEFAULT 1 COMMENT ''状态：0 停用 1 启用''');

-- 2.5 优惠券：创建方
CALL add_column_if_not_exists('coupon', 'creator_type', 'TINYINT NOT NULL DEFAULT 1 COMMENT ''创建方：1 平台 2 商家''');

-- 2.6 订单：各状态时间节点
CALL add_column_if_not_exists('orders', 'accept_time', 'DATETIME DEFAULT NULL COMMENT ''接单时间''');
CALL add_column_if_not_exists('orders', 'prepare_time', 'DATETIME DEFAULT NULL COMMENT ''开始备餐时间''');
CALL add_column_if_not_exists('orders', 'cancel_time', 'DATETIME DEFAULT NULL COMMENT ''取消时间''');

-- 2.7 订单状态注释扩展（6 退款中 / 7 已退款）
ALTER TABLE `orders`
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0
  COMMENT '状态：0 待支付 1 待接单 2 备餐中 3 配送中 4 已完成 5 已取消 6 退款中 7 已退款';

-- 2.8 支付单状态注释扩展（3 已退款）
ALTER TABLE `payment`
  MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 0
  COMMENT '状态：0 待支付 1 已支付 2 已取消 3 已退款';

-- -------------------------------------------------------------
-- 3. 基础数据（幂等）
-- -------------------------------------------------------------
-- 管理员：admin / admin123（仅本地联调，上线必须改密）
INSERT INTO `admin` (`username`, `password`, `salt`, `real_name`, `role`, `status`)
VALUES ('admin', 'cY/9f+6CcPS3xVhEJ9QDChsP5ImL3ISntW6mfelTILo=', 'a3qcHS4/QFFic4SVprfI2Q==', '平台管理员', 1, 1)
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

-- 平台默认配置
INSERT INTO `platform_config` (`config_key`, `config_value`, `remark`) VALUES
('commission_rate', '0.05', '默认抽佣比例（0-0.3）'),
('default_delivery_fee', '2.00', '默认配送费'),
('default_min_order_amount', '15.00', '默认起送价'),
('auto_accept', '0', '全局自动接单：0 关 1 开'),
('home_notice', '欢迎使用橙盟美食城，下单立享优惠', '首页公告')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

-- -------------------------------------------------------------
-- 4. 清理工具存储过程
-- -------------------------------------------------------------
DROP PROCEDURE IF EXISTS `add_column_if_not_exists`;
