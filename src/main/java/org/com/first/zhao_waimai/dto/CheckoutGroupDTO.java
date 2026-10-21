package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 结算响应中的子单分组（一个档口一组）
 */
@Data
public class CheckoutGroupDTO {

    /** 档口 id */
    private Long shopId;

    /** 子订单 id（预览时为 null，提交后回填） */
    private Long orderId;

    /** 档口名称 */
    private String shopName;

    /** 商品总额 */
    private BigDecimal totalAmount;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 本单分摊的优惠金额 */
    private BigDecimal discountAmount;

    /** 实付 = 商品总额 + 配送费 - 优惠 */
    private BigDecimal payAmount;

    /** 预计送达分钟数 */
    private Integer averageDeliveryTime;

    /** 本组订单项 */
    private List<CheckoutItemVO> items;
}
