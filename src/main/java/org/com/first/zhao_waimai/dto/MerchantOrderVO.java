package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 商家端订单
 */
@Data
public class MerchantOrderVO {

    private Long orderId;
    private String orderNo;
    private Integer status;
    private BigDecimal totalAmount;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String addressSnapshot;
    private String remark;
    private LocalDateTime createTime;
    private List<OrderItemVO> items;
}
