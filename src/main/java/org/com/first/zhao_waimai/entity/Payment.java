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
 * 支付单（一次结算对应一个）
 */
@Data
@TableName("payment")
public class Payment {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 支付单号（唯一） */
    private String paymentNo;

    /** 用户 id */
    private Long userId;

    /** 应付总额（各子单实付之和） */
    private BigDecimal totalAmount;

    /** 支付渠道：1 模拟 2 微信支付 */
    private Integer channel;

    /** 状态：0 待支付 1 已支付 2 已取消 */
    private Integer status;

    /** 支付成功时间 */
    private LocalDateTime payTime;

    /** 第三方支付流水号 */
    private String transactionId;

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
