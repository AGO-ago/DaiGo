package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.CouponVO;
import org.com.first.zhao_waimai.dto.UserCouponVO;
import org.com.first.zhao_waimai.service.CouponService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 优惠券接口
 */
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /**
     * 领券中心
     */
    @GetMapping("/coupon/list")
    public Result<List<CouponVO>> list() {
        return Result.ok(couponService.listAvailable(UserContext.getUserId()));
    }

    /**
     * 领取优惠券
     */
    @PostMapping("/coupon/{id}/receive")
    public Result<Void> receive(@PathVariable Long id) {
        couponService.receive(UserContext.getUserId(), id);
        return Result.ok();
    }

    /**
     * 我的优惠券（status 默认 0 未使用）
     */
    @GetMapping("/user/coupon")
    public Result<List<UserCouponVO>> myCoupons(@RequestParam(required = false, defaultValue = "0") Integer status) {
        return Result.ok(couponService.myCoupons(UserContext.getUserId(), status));
    }
}
