-- =============================================================
-- 美食城外卖点餐平台 全量建表脚本
-- MySQL 8.x / utf8mb4
-- 通用约定（对齐 AGENTS 第六节）：
--   每表必有 id bigint 主键、create_time、update_time、deleted 逻辑删除
--   所有表/字段写注释；索引每表 <=5
-- 说明：设计稿中的 order 表因与 MySQL 保留字冲突，定名 orders
-- =============================================================

CREATE DATABASE IF NOT EXISTS zhao_waimai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE zhao_waimai;

-- -------------------------------------------------------------
-- 1. 美食城/商圈
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `mall`;
CREATE TABLE `mall` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`            VARCHAR(64)  NOT NULL COMMENT '美食城/商圈名称',
  `address`         VARCHAR(200) NOT NULL COMMENT '详细地址',
  `longitude`       DECIMAL(10,7)         DEFAULT NULL COMMENT '经度',
  `latitude`        DECIMAL(10,7)         DEFAULT NULL COMMENT '纬度',
  `cover_img`       VARCHAR(500)          DEFAULT NULL COMMENT '封面图 URL',
  `business_hours`  VARCHAR(64)           DEFAULT NULL COMMENT '营业时间，如 08:00-20:00',
  `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0 关闭 1 营业',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB COMMENT = '美食城/商圈';

-- -------------------------------------------------------------
-- 2. 档口/商家
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `shop`;
CREATE TABLE `shop` (
  `id`                    BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `mall_id`               BIGINT        NOT NULL COMMENT '所属美食城 id',
  `name`                  VARCHAR(64)   NOT NULL COMMENT '档口名称',
  `logo`                  VARCHAR(500)           DEFAULT NULL COMMENT '档口 logo URL',
  `phone`                 VARCHAR(20)            DEFAULT NULL COMMENT '联系电话',
  `delivery_fee`          DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '固定配送费',
  `min_order_amount`      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '起送价',
  `average_delivery_time` INT                    DEFAULT 30 COMMENT '预计送达分钟数',
  `business_hours`        VARCHAR(64)            DEFAULT NULL COMMENT '营业时间',
  `status`                TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0 休息 1 营业',
  `announcement`          VARCHAR(200)           DEFAULT NULL COMMENT '档口公告',
  `auto_accept`           TINYINT       NOT NULL DEFAULT 0 COMMENT '自动接单：0 否 1 是',
  `commission_rate`       DECIMAL(5,4)           DEFAULT NULL COMMENT '抽佣比例，空取平台默认',
  `merchant_user_id`      BIGINT                 DEFAULT NULL COMMENT '绑定商家账号 id（平台开通后写入）',
  `create_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`               TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_mall_id` (`mall_id`),
  KEY `idx_merchant_user_id` (`merchant_user_id`)
) ENGINE = InnoDB COMMENT = '档口/商家';

-- -------------------------------------------------------------
-- 3. 菜品分类
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `dish_category`;
CREATE TABLE `dish_category` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`     BIGINT      NOT NULL COMMENT '所属档口 id',
  `name`        VARCHAR(32) NOT NULL COMMENT '分类名称',
  `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE = InnoDB COMMENT = '菜品分类';

-- -------------------------------------------------------------
-- 4. 菜品
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `shop_id`        BIGINT        NOT NULL COMMENT '所属档口 id',
  `category_id`    BIGINT        NOT NULL COMMENT '所属分类 id',
  `name`           VARCHAR(64)   NOT NULL COMMENT '菜品名称',
  `image`          VARCHAR(500)           DEFAULT NULL COMMENT '菜品图 URL',
  `price`          DECIMAL(10,2) NOT NULL COMMENT '售价',
  `original_price` DECIMAL(10,2)          DEFAULT NULL COMMENT '原价（划线价，可空）',
  `description`    VARCHAR(500)           DEFAULT NULL COMMENT '菜品描述',
  `status`         TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0 下架 1 上架',
  `sort`           INT           NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `sold_count`     INT           NOT NULL DEFAULT 0 COMMENT '累计销量',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE = InnoDB COMMENT = '菜品';

-- -------------------------------------------------------------
-- 5. 菜品规格/口味
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `dish_spec`;
CREATE TABLE `dish_spec` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `dish_id`     BIGINT        NOT NULL COMMENT '所属菜品 id',
  `name`        VARCHAR(32)   NOT NULL COMMENT '规格名，如 微辣/大份',
  `price`       DECIMAL(10,2)          DEFAULT NULL COMMENT '加价（可空为不加价）',
  `sort`        INT           NOT NULL DEFAULT 0 COMMENT '排序值，越小越靠前',
  `status`      TINYINT       NOT NULL DEFAULT 1 COMMENT '状态：0 停用 1 启用',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_dish_id` (`dish_id`)
) ENGINE = InnoDB COMMENT = '菜品规格/口味';

-- -------------------------------------------------------------
-- 6. 用户
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  `openid`      VARCHAR(64) NOT NULL COMMENT '微信 openid（按小程序隔离）',
  `nickname`    VARCHAR(64)          DEFAULT NULL COMMENT '昵称',
  `avatar`      VARCHAR(500)         DEFAULT NULL COMMENT '头像 URL',
  `phone`       VARCHAR(20)          DEFAULT NULL COMMENT '手机号',
  `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：0 禁用 1 正常',
  `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`)
) ENGINE = InnoDB COMMENT = '用户';

-- -------------------------------------------------------------
-- 7. 用户地址（写字楼工位）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT       NOT NULL COMMENT '所属用户 id',
  `contact`     VARCHAR(32)  NOT NULL COMMENT '联系人姓名',
  `company`     VARCHAR(64)  NOT NULL COMMENT '公司名称',
  `building`    VARCHAR(32)  NOT NULL COMMENT '楼栋',
  `floor`       VARCHAR(16)           DEFAULT NULL COMMENT '楼层',
  `office`      VARCHAR(32)           DEFAULT NULL COMMENT '工位/房号',
  `phone`       VARCHAR(20)  NOT NULL COMMENT '联系电话',
  `is_default`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否默认：0 否 1 是',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '用户地址（写字楼工位）';

-- -------------------------------------------------------------
-- 8. 优惠券模板
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `coupon`;
CREATE TABLE `coupon` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name`         VARCHAR(64)   NOT NULL COMMENT '券名称',
  `type`         TINYINT       NOT NULL COMMENT '类型：1 满减 2 折扣',
  `threshold`    DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '使用门槛金额（满 X 可用，0 无门槛）',
  `amount`       DECIMAL(10,2) NOT NULL COMMENT '减免额（满减）或折扣率（折扣）',
  `total_count`  INT           NOT NULL DEFAULT 0 COMMENT '发放总量',
  `remain_count` INT           NOT NULL DEFAULT 0 COMMENT '剩余数量',
  `start_time`   DATETIME      NOT NULL COMMENT '生效时间',
  `end_time`     DATETIME      NOT NULL COMMENT '失效时间',
  `shop_id`      BIGINT                 DEFAULT NULL COMMENT '限定档口 id（空为平台通用券）',
  `creator_type` TINYINT       NOT NULL DEFAULT 1 COMMENT '创建方：1 平台 2 商家',
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE = InnoDB COMMENT = '优惠券模板';

-- -------------------------------------------------------------
-- 9. 用户领券
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `user_coupon`;
CREATE TABLE `user_coupon` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL COMMENT '用户 id',
  `coupon_id`   BIGINT   NOT NULL COMMENT '券模板 id',
  `status`      TINYINT  NOT NULL DEFAULT 0 COMMENT '状态：0 未使用 1 已使用 2 已过期',
  `use_time`    DATETIME          DEFAULT NULL COMMENT '使用时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_coupon_id` (`coupon_id`)
) ENGINE = InnoDB COMMENT = '用户领券';

-- -------------------------------------------------------------
-- 10. 支付单（一次结算对应一个支付单）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `payment`;
CREATE TABLE `payment` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payment_no`      VARCHAR(32)   NOT NULL COMMENT '支付单号（唯一）',
  `user_id`         BIGINT        NOT NULL COMMENT '用户 id',
  `total_amount`    DECIMAL(10,2) NOT NULL COMMENT '应付总额（各子单实付之和）',
  `channel`         TINYINT       NOT NULL DEFAULT 1 COMMENT '支付渠道：1 模拟 2 微信支付',
  `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待支付 1 已支付 2 已取消 3 已退款',
  `pay_time`        DATETIME               DEFAULT NULL COMMENT '支付成功时间',
  `transaction_id`  VARCHAR(64)            DEFAULT NULL COMMENT '第三方支付流水号',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_no` (`payment_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '支付单';

-- -------------------------------------------------------------
-- 11. 子订单（按档口拆分）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`         VARCHAR(32)   NOT NULL COMMENT '子订单号（唯一）',
  `payment_id`       BIGINT        NOT NULL COMMENT '所属支付单 id',
  `user_id`          BIGINT        NOT NULL COMMENT '下单用户 id',
  `shop_id`          BIGINT        NOT NULL COMMENT '档口 id（拆单维度）',
  `status`           TINYINT       NOT NULL DEFAULT 0 COMMENT '状态：0 待支付 1 待接单 2 备餐中 3 配送中 4 已完成 5 已取消 6 退款中 7 已退款',
  `total_amount`     DECIMAL(10,2) NOT NULL COMMENT '商品总额',
  `delivery_fee`     DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '配送费',
  `discount_amount`  DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '优惠金额',
  `pay_amount`       DECIMAL(10,2) NOT NULL COMMENT '实付金额 = 商品总额 + 配送费 - 优惠',
  `address_snapshot` VARCHAR(500)  NOT NULL COMMENT '收货信息快照（联系人/公司/楼栋/楼层/工位/电话）',
  `remark`           VARCHAR(200)           DEFAULT NULL COMMENT '订单备注',
  `accept_time`      DATETIME               DEFAULT NULL COMMENT '接单时间',
  `prepare_time`     DATETIME               DEFAULT NULL COMMENT '开始备餐时间',
  `delivery_time`    DATETIME               DEFAULT NULL COMMENT '开始配送时间',
  `finish_time`      DATETIME               DEFAULT NULL COMMENT '完成时间',
  `cancel_time`      DATETIME               DEFAULT NULL COMMENT '取消时间',
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`          TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_shop_id_status` (`shop_id`, `status`),
  KEY `idx_payment_id` (`payment_id`)
) ENGINE = InnoDB COMMENT = '子订单（按档口拆分）';

-- -------------------------------------------------------------
-- 12. 订单项（下单时快照）
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT        NOT NULL COMMENT '所属子订单 id',
  `dish_id`     BIGINT        NOT NULL COMMENT '菜品 id',
  `dish_name`   VARCHAR(64)   NOT NULL COMMENT '菜品名称快照',
  `spec_id`     BIGINT                 DEFAULT NULL COMMENT '规格 id',
  `spec_name`   VARCHAR(64)            DEFAULT NULL COMMENT '规格名快照',
  `spec_price`  DECIMAL(10,2)          DEFAULT NULL COMMENT '规格加价快照',
  `dish_image`  VARCHAR(500)           DEFAULT NULL COMMENT '菜品图快照',
  `price`       DECIMAL(10,2) NOT NULL COMMENT '成交单价快照',
  `count`       INT           NOT NULL COMMENT '数量',
  `amount`      DECIMAL(10,2) NOT NULL COMMENT '小计 = 单价 * 数量',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB COMMENT = '订单项（快照）';

-- -------------------------------------------------------------
-- 13. 订单用券记录
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_coupon`;
CREATE TABLE `order_coupon` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`        BIGINT        NOT NULL COMMENT '子订单 id',
  `user_coupon_id`  BIGINT        NOT NULL COMMENT '用户券 id',
  `amount`          DECIMAL(10,2) NOT NULL COMMENT '本单分摊的优惠金额',
  `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`)
) ENGINE = InnoDB COMMENT = '订单用券记录';

-- -------------------------------------------------------------
-- 14. 订单评价
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_comment`;
CREATE TABLE `order_comment` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_id`    BIGINT       NOT NULL COMMENT '子订单 id',
  `user_id`     BIGINT       NOT NULL COMMENT '评价用户 id',
  `shop_id`     BIGINT       NOT NULL COMMENT '档口 id',
  `score`       TINYINT      NOT NULL COMMENT '评分 1-5',
  `content`     VARCHAR(500)          DEFAULT NULL COMMENT '评价内容',
  `images`        VARCHAR(1000)         DEFAULT NULL COMMENT '评价图，逗号分隔 URL',
  `reply_content` VARCHAR(200)          DEFAULT NULL COMMENT '商家回复',
  `reply_time`    DATETIME              DEFAULT NULL COMMENT '商家回复时间',
  `admin_reply`   VARCHAR(200)          DEFAULT NULL COMMENT '平台回复',
  `hidden`        TINYINT     NOT NULL DEFAULT 0 COMMENT '是否隐藏：0 否 1 是',
  `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT     NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_shop_id` (`shop_id`)
) ENGINE = InnoDB COMMENT = '订单评价';

-- -------------------------------------------------------------
-- 15. 收藏
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
  `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id`     BIGINT   NOT NULL COMMENT '用户 id',
  `shop_id`     BIGINT   NOT NULL COMMENT '档口 id',
  `type`        TINYINT  NOT NULL DEFAULT 1 COMMENT '类型：1 档口（预留 2 菜品）',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     TINYINT  NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删 1 已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_shop_type` (`user_id`, `shop_id`, `type`)
) ENGINE = InnoDB COMMENT = '收藏';

-- -------------------------------------------------------------
-- 16. 平台管理员
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `admin`;
CREATE TABLE `admin` (
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

-- -------------------------------------------------------------
-- 17. 商家入驻申请
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `merchant_apply`;
CREATE TABLE `merchant_apply` (
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

-- -------------------------------------------------------------
-- 18. 退款单
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `order_refund`;
CREATE TABLE `order_refund` (
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

-- -------------------------------------------------------------
-- 19. 结算单
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `settlement`;
CREATE TABLE `settlement` (
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

-- -------------------------------------------------------------
-- 20. 平台配置
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `platform_config`;
CREATE TABLE `platform_config` (
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

-- -------------------------------------------------------------
-- 21. 秒杀活动
-- -------------------------------------------------------------
DROP TABLE IF EXISTS `seckill_activity`;
CREATE TABLE `seckill_activity` (
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
