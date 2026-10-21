package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.CheckoutRequestDTO;
import org.com.first.zhao_waimai.dto.CheckoutResultDTO;
import org.com.first.zhao_waimai.dto.OrderDetailVO;
import org.com.first.zhao_waimai.dto.OrderListVO;
import org.com.first.zhao_waimai.dto.PageDTO;

/**
 * 订单服务：购物车校验 + 跨档口拆单 + 支付回调 + 订单查询
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

    /**
     * 模拟支付：支付单置已支付，其下待支付子单置待接单
     */
    void pay(Long userId, Long paymentId);

    /**
     * 我的订单分页列表（status 为空则全部）
     */
    PageDTO<OrderListVO> listOrders(Long userId, Integer status, long page, long size);

    /**
     * 订单详情（仅本人）
     */
    OrderDetailVO getDetail(Long userId, Long orderId);

    /**
     * 取消订单（仅本人、仅待支付可取消），支付单同步取消
     */
    void cancel(Long userId, Long orderId);
}
