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
 * 订单用券记录（跨档口拆单时按子单分摊）
 */
@Data
@TableName("order_coupon")
public class OrderCoupon {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 子订单 id */
    private Long orderId;

    /** 用户券 id */
    private Long userCouponId;

    /** 本单分摊的优惠金额 */
    private BigDecimal amount;

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
