package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.MerchantBillVO;
import org.com.first.zhao_waimai.dto.MerchantOrderDetailVO;
import org.com.first.zhao_waimai.dto.MerchantRefundVO;
import org.com.first.zhao_waimai.entity.Settlement;

import java.util.List;

/**
 * 商家端交易闭环：订单详情、拒单、退款审批、账单与结算单
 */
public interface MerchantTradeService {

    /**
     * 订单详情（含规格明细与状态时间轴）
     */
    MerchantOrderDetailVO orderDetail(Long userId, Long orderId);

    /**
     * 拒单（仅待接单可拒），状态 1→5 并生成已退款退款单
     */
    void rejectOrder(Long userId, Long orderId, String reason);

    /**
     * 退款申请列表（status 为空查全部）
     */
    List<MerchantRefundVO> listRefunds(Long userId, Integer status);

    /**
     * 同意退款：订单 6→7、退款单置已退款
     */
    void agreeRefund(Long userId, Long refundId);

    /**
     * 拒绝退款：退款单置已拒绝，订单回到申请前状态
     */
    void rejectRefund(Long userId, Long refundId, String reason);

    /**
     * 账单汇总与明细，period 取 today / 7d / month
     */
    MerchantBillVO bill(Long userId, String period);

    /**
     * 平台结算单列表（只读）
     */
    List<Settlement> settlements(Long userId);
}
