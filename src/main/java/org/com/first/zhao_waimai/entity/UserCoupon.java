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
 * 用户领取的优惠券
 */
@Data
@TableName("user_coupon")
public class UserCoupon {

    /** 状态：未使用 */
    public static final int STATUS_UNUSED = 0;
    /** 状态：已使用 */
    public static final int STATUS_USED = 1;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 id */
    private Long userId;

    /** 券模板 id */
    private Long couponId;

    /** 状态：0 未使用 1 已使用 2 已过期 */
    private Integer status;

    /** 使用时间 */
    private LocalDateTime useTime;

    /** 领取时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删 1 已删 */
    @TableLogic
    private Integer deleted;
}
