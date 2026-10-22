package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 平台看板汇总指标
 */
@Data
@Builder
public class DashboardSummaryVO {

    /** 今日交易额（已支付且未取消/退款） */
    private BigDecimal todayGmv;

    /** 今日订单数 */
    private Long todayOrderCount;

    /** 今日客单价 */
    private BigDecimal todayAvgAmount;

    /** 今日新增用户 */
    private Long todayNewUsers;

    /** 累计交易额 */
    private BigDecimal totalGmv;

    /** 累计订单数 */
    private Long totalOrders;

    /** 待审核入驻申请 */
    private Long pendingApplyCount;

    /** 待结算单数 */
    private Long pendingSettlementCount;

    /** 待处理差评（1-2 星且未隐藏） */
    private Long badCommentCount;

    /** 营业中档口数 */
    private Long onlineShopCount;
}
