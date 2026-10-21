package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.CheckoutRequestDTO;
import org.com.first.zhao_waimai.dto.CheckoutResultDTO;
import org.com.first.zhao_waimai.dto.OrderDetailVO;
import org.com.first.zhao_waimai.dto.OrderListVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.service.OrderService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口（用户端）
 */
@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * 结算预览：校验购物车并按档口拆单预览，不落库
     */
    @PostMapping("/checkout")
    public Result<CheckoutResultDTO> checkout(@Valid @RequestBody CheckoutRequestDTO request) {
        return Result.ok(orderService.checkout(UserContext.getUserId(), request));
    }

    /**
     * 提交订单：创建支付单 + 按档口拆子单（同事务）
     */
    @PostMapping("/submit")
    public Result<CheckoutResultDTO> submit(@Valid @RequestBody CheckoutRequestDTO request) {
        return Result.ok(orderService.submit(UserContext.getUserId(), request));
    }

    /**
     * 模拟支付（预留真实微信支付适配）
     */
    @PostMapping("/pay/{paymentId}")
    public Result<Void> pay(@PathVariable Long paymentId) {
        orderService.pay(UserContext.getUserId(), paymentId);
        return Result.ok(null);
    }

    /**
     * 我的订单列表（status 为空查全部）
     */
    @GetMapping("/list")
    public Result<PageDTO<OrderListVO>> list(@RequestParam(required = false) Integer status,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.listOrders(UserContext.getUserId(), status, page, size));
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Long id) {
        return Result.ok(orderService.getDetail(UserContext.getUserId(), id));
    }

    /**
     * 取消订单（仅待支付）
     */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(UserContext.getUserId(), id);
        return Result.ok(null);
    }
}
