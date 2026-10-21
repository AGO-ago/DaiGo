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
 * 子订单（按档口拆分）
 */
@Data
@TableName("orders")
public class Orders {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 子订单号（唯一） */
    private String orderNo;

    /** 所属支付单 id */
    private Long paymentId;

    /** 下单用户 id */
    private Long userId;

    /** 档口 id（拆单维度） */
    private Long shopId;

    /** 状态：0 待支付 1 待接单 2 备餐中 3 配送中 4 已完成 5 已取消 */
    private Integer status;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 = 商品总额 + 配送费 - 优惠 */
    private BigDecimal payAmount;

    /** 收货信息快照 */
    private String addressSnapshot;

    /** 订单备注 */
    private String remark;

    /** 开始配送时间 */
    private LocalDateTime deliveryTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

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
