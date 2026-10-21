package org.com.first.zhao_waimai.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单详情响应
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailVO extends OrderListVO {

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 优惠金额 */
    private BigDecimal discountAmount;

    /** 收货信息快照 */
    private String addressSnapshot;

    /** 备注 */
    private String remark;

    /** 所属支付单 id */
    private Long paymentId;
}
