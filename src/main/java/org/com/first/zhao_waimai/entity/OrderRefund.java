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
 * 退款单
 */
@Data
@TableName("order_refund")
public class OrderRefund {

    /** 状态：申请中 */
    public static final int STATUS_APPLYING = 0;
    /** 状态：已同意（待退款） */
    public static final int STATUS_AGREED = 1;
    /** 状态：已拒绝 */
    public static final int STATUS_REJECTED = 2;
    /** 状态：已退款 */
    public static final int STATUS_REFUNDED = 3;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 退款单号（唯一） */
    private String refundNo;

    /** 子订单 id */
    private Long orderId;

    /** 支付单 id */
    private Long paymentId;

    /** 申请用户 id */
    private Long userId;

    /** 档口 id */
    private Long shopId;

    /** 退款金额 */
    private BigDecimal amount;

    /** 退款原因 */
    private String reason;

    /** 状态：0 申请中 1 已同意 2 已拒绝 3 已退款 */
    private Integer status;

    /** 申请前订单状态 */
    private Integer prevStatus;

    /** 拒绝原因 */
    private String rejectReason;

    /** 处理管理员 id */
    private Long handleAdminId;

    /** 处理时间 */
    private LocalDateTime handleTime;

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
