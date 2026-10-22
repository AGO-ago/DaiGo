package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 平台端订单展示（列表不带明细，详情带 items）
 */
@Data
public class AdminOrderVO {

    /** 子订单 id */
    private Long id;

    /** 子订单号 */
    private String orderNo;

    /** 支付单 id */
    private Long paymentId;

    /** 下单用户 id */
    private Long userId;

    /** 下单用户昵称 */
    private String userName;

    /** 档口 id */
    private Long shopId;

    /** 档口名称 */
    private String shopName;

    /** 状态：0 待支付 1 待接单 2 备餐中 3 配送中 4 已完成 5 已取消 6 退款中 7 已退款 */
    private Integer status;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 收货信息快照 */
    private String addressSnapshot;

    /** 备注 */
    private String remark;

    /** 商品摘要 */
    private String itemSummary;

    /** 接单时间 */
    private LocalDateTime acceptTime;

    /** 完成时间 */
    private LocalDateTime finishTime;

    /** 取消时间 */
    private LocalDateTime cancelTime;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 订单明细（仅详情返回） */
    private List<OrderItemVO> items;
}
