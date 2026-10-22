package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 入驻申请审核通过：可绑定已有空档口，或按申请信息新建档口
 */
@Data
public class ApplyApproveDTO {

    /** 已有空档口 id（优先） */
    private Long shopId;

    /** 新建档口时的配送费 */
    private BigDecimal deliveryFee;

    /** 新建档口时的起送价 */
    private BigDecimal minOrderAmount;

    /** 新建档口时的预计送达分钟数 */
    private Integer averageDeliveryTime;
}
