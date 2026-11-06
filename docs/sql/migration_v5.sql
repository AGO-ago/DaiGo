-- =============================================================
-- 代go V5 增量迁移：平台运营参数扩充（行业标准基础配置）
-- 适用：已有数据的本地/测试/线上库（保留数据）
-- 特性：幂等，可重复执行（依赖 platform_config.uk_config_key 唯一键）
-- 执行：mysql -uroot -p zhao_waimai -e "source docs/sql/migration_v5.sql"
-- =============================================================
USE zhao_waimai;

INSERT IGNORE INTO `platform_config` (`config_key`, `config_value`, `remark`) VALUES
    ('commission_rate', '0.05', '默认抽佣比例（0-0.30，档口单独设置优先）'),
    ('default_delivery_fee', '2.00', '默认配送费（新档口默认值）'),
    ('default_min_order_amount', '15.00', '默认起送价（新档口默认值）'),
    ('auto_accept', '0', '全局自动接单：0 关 1 开'),
    ('home_notice', '', '首页公告'),
    ('support_phone', '4000000000', '客服电话（用户端“联系客服”拨打）'),
    ('business_hours', '09:00-22:00', '营业时间，格式 HH:mm-HH:mm'),
    ('order_pay_timeout_minutes', '15', '待支付超时自动取消（分钟，1-120）'),
    ('comment_days_limit', '7', '订单完成后可评价时限（天，1-30）');

SELECT `config_key`, `config_value`, `remark` FROM `platform_config` ORDER BY `id`;
