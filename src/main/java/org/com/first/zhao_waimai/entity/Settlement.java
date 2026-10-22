package org.com.first.zhao_waimai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单
 */
@Data
@TableName("settlement")
public class Settlement {

    /** 状态：待结算 */
    public static final int STATUS_PENDING = 0;
    /** 状态：已结算 */
    public static final int STATUS_SETTLED = 1;

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 结算单号（唯一） */
    private String settlementNo;

    /** 档口 id */
    private Long shopId;

    /** 结算周期开始 */
    private LocalDate periodStart;

    /** 结算周期结束 */
    private LocalDate periodEnd;

    /** 订单数 */
    private Integer orderCount;

    /** 商品金额 */
    private BigDecimal goodsAmount;

    /** 配送费 */
    private BigDecimal deliveryAmount;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 抽佣比例 */
    private BigDecimal commissionRate;

    /** 抽佣金额 */
    private BigDecimal commissionAmount;

    /** 退款金额 */
    private BigDecimal refundAmount;

    /** 应结金额 */
    private BigDecimal netAmount;

    /** 状态：0 待结算 1 已结算 */
    private Integer status;

    /** 打款/结算时间 */
    private LocalDateTime settleTime;

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
