package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.MerchantBillVO;
import org.com.first.zhao_waimai.dto.MerchantOrderDetailVO;
import org.com.first.zhao_waimai.dto.MerchantRefundVO;
import org.com.first.zhao_waimai.dto.ReasonDTO;
import org.com.first.zhao_waimai.entity.Settlement;
import org.com.first.zhao_waimai.service.MerchantTradeService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商家端交易接口：订单详情、拒单、退款审批、账单、结算单
 */
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantTradeController {

    private final MerchantTradeService merchantTradeService;

    /**
     * 订单详情（含规格与状态时间轴）
     */
    @GetMapping("/order/{id}")
    public Result<MerchantOrderDetailVO> orderDetail(@PathVariable Long id) {
        return Result.ok(merchantTradeService.orderDetail(UserContext.getUserId(), id));
    }

    /**
     * 拒单（仅待接单）
     */
    @PutMapping("/order/{id}/reject")
    public Result<Void> rejectOrder(@PathVariable Long id, @Valid @RequestBody ReasonDTO dto) {
        merchantTradeService.rejectOrder(UserContext.getUserId(), id, dto.getReason());
        return Result.ok(null);
    }

    /**
     * 退款申请列表
     */
    @GetMapping("/refunds")
    public Result<List<MerchantRefundVO>> refunds(@RequestParam(required = false) Integer status) {
        return Result.ok(merchantTradeService.listRefunds(UserContext.getUserId(), status));
    }

    /**
     * 同意退款
     */
    @PutMapping("/refund/{id}/agree")
    public Result<Void> agreeRefund(@PathVariable Long id) {
        merchantTradeService.agreeRefund(UserContext.getUserId(), id);
        return Result.ok(null);
    }

    /**
     * 拒绝退款
     */
    @PutMapping("/refund/{id}/reject")
    public Result<Void> rejectRefund(@PathVariable Long id, @Valid @RequestBody ReasonDTO dto) {
        merchantTradeService.rejectRefund(UserContext.getUserId(), id, dto.getReason());
        return Result.ok(null);
    }

    /**
     * 账单汇总 + 明细：period = today / 7d / month
     */
    @GetMapping("/bill")
    public Result<MerchantBillVO> bill(@RequestParam(defaultValue = "today") String period) {
        return Result.ok(merchantTradeService.bill(UserContext.getUserId(), period));
    }

    /**
     * 平台结算单列表（只读）
     */
    @GetMapping("/settlements")
    public Result<List<Settlement>> settlements() {
        return Result.ok(merchantTradeService.settlements(UserContext.getUserId()));
    }
}
