package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 商家今日经营统计
 */
@Data
@Builder
public class MerchantStatVO {

    /** 今日营收（已支付及以后状态子单实付之和） */
    private BigDecimal todayAmount;

    /** 今日订单数 */
    private Integer todayOrderCount;

    /** 待接单数 */
    private Integer pendingCount;

    /** 今日已完成数 */
    private Integer finishedCount;
}
