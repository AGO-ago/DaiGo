package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 平台端结算单展示
 */
@Data
public class SettlementVO {

    /** 结算单 id */
    private Long id;

    /** 结算单号 */
    private String settlementNo;

    /** 档口 id */
    private Long shopId;

    /** 档口名称 */
    private String shopName;

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

    /** 结算时间 */
    private LocalDateTime settleTime;

    /** 创建时间 */
    private LocalDateTime createTime;
}
