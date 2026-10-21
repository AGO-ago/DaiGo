package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.CheckoutRequestDTO;
import org.com.first.zhao_waimai.dto.CheckoutResultDTO;
import org.com.first.zhao_waimai.service.OrderService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
