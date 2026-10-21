package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表项响应
 */
@Data
public class OrderListVO {

    /** 子订单 id */
    private Long orderId;

    /** 子订单号 */
    private String orderNo;

    /** 档口 id */
    private Long shopId;

    /** 档口名 */
    private String shopName;

    /** 状态：0 待支付 1 待接单 2 备餐中 3 配送中 4 已完成 5 已取消 */
    private Integer status;

    /** 实付金额 */
    private BigDecimal payAmount;

    /** 下单时间 */
    private LocalDateTime createTime;

    /** 订单项 */
    private List<OrderItemVO> items;
}
