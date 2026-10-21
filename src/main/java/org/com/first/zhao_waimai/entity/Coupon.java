package org.com.first.zhao_waimai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券模板
 */
@Data
@TableName("coupon")
public class Coupon {

    /** 类型：满减 */
    public static final int TYPE_THRESHOLD = 1;
    /** 类型：折扣 */
    public static final int TYPE_DISCOUNT = 2;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 券名称 */
    private String name;

    /** 类型：1 满减 2 折扣 */
    private Integer type;

    /** 使用门槛金额（满 X 可用，0 无门槛） */
    private BigDecimal threshold;

    /** 减免额（满减）或折扣率（折扣，0.85 表示 85 折） */
    private BigDecimal amount;

    /** 发放总量 */
    private Integer totalCount;

    /** 剩余数量 */
    private Integer remainCount;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 限定档口 id（空为平台通用券） */
    private Long shopId;

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
