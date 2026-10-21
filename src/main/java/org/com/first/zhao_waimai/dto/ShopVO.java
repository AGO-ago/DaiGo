package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 档口列表项响应
 */
@Data
@Builder
public class ShopVO {

    /** 档口 id */
    private Long id;

    /** 档口名称 */
    private String name;

    /** 档口 logo URL */
    private String logo;

    /** 配送费 */
    private BigDecimal deliveryFee;

    /** 起送价 */
    private BigDecimal minOrderAmount;

    /** 预计送达分钟数 */
    private Integer averageDeliveryTime;

    /** 营业时间 */
    private String businessHours;

    /** 状态：0 休息 1 营业 */
    private Integer status;
}
