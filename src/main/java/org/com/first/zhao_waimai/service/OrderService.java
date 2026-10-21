package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.CheckoutRequestDTO;
import org.com.first.zhao_waimai.dto.CheckoutResultDTO;

/**
 * 订单服务：购物车校验 + 跨档口拆单
 */
public interface OrderService {

    /**
     * 结算预览：校验购物车（下架/休息/起送价）并按档口拆单，不落库
     */
    CheckoutResultDTO checkout(Long userId, CheckoutRequestDTO request);

    /**
     * 提交订单：一个支付单 + N 个档口子单（同事务）
     */
    CheckoutResultDTO submit(Long userId, CheckoutRequestDTO request);
}
