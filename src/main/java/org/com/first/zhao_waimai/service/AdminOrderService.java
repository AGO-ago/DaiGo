package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AdminOrderVO;
import org.com.first.zhao_waimai.dto.PageDTO;

import java.time.LocalDate;

/**
 * 平台端订单监控
 */
public interface AdminOrderService {

    /**
     * 分页查询订单
     */
    PageDTO<AdminOrderVO> list(String keyword, Long shopId, Integer status,
                               LocalDate startDate, LocalDate endDate, long page, long size);

    /**
     * 订单详情（含明细）
     */
    AdminOrderVO detail(Long orderId);

    /**
     * 强制取消：未支付直接取消，已支付退款并退券
     */
    void cancel(Long adminId, Long orderId);
}
