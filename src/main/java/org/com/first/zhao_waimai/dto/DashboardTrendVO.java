package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台看板趋势点
 */
@Data
@Builder
public class DashboardTrendVO {

    /** 日期 yyyy-MM-dd */
    private String date;

    /** 交易额 */
    private BigDecimal gmv;

    /** 订单数 */
    private Long orderCount;
}
