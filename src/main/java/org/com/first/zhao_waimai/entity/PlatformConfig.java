package org.com.first.zhao_waimai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台配置（键值对）
 */
@Data
@TableName("platform_config")
public class PlatformConfig {

    /** 默认抽佣比例 */
    public static final String KEY_COMMISSION_RATE = "commission_rate";
    /** 默认配送费 */
    public static final String KEY_DELIVERY_FEE = "default_delivery_fee";
    /** 默认起送价 */
    public static final String KEY_MIN_ORDER_AMOUNT = "default_min_order_amount";
    /** 全局自动接单 */
    public static final String KEY_AUTO_ACCEPT = "auto_accept";
    /** 首页公告 */
    public static final String KEY_HOME_NOTICE = "home_notice";
    /** 客服电话 */
    public static final String KEY_SUPPORT_PHONE = "support_phone";
    /** 营业时间（HH:mm-HH:mm） */
    public static final String KEY_BUSINESS_HOURS = "business_hours";
    /** 待支付超时自动取消（分钟） */
    public static final String KEY_PAY_TIMEOUT_MINUTES = "order_pay_timeout_minutes";
    /** 订单评价时限（天） */
    public static final String KEY_COMMENT_DAYS = "comment_days_limit";

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 说明 */
    private String remark;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
