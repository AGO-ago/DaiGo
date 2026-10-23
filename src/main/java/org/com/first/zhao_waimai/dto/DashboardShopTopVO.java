package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台看板档口排行
 */
@Data
@Builder
public class DashboardShopTopVO {

    /** 档口 ID */
    private Long shopId;

    /** 档口名称 */
    private String shopName;

    /** 交易额 */
    private BigDecimal gmv;

    /** 订单数 */
    private Long orderCount;
}
