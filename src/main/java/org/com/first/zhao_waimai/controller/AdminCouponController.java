package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.CouponSaveDTO;
import org.com.first.zhao_waimai.dto.MerchantCouponVO;
import org.com.first.zhao_waimai.service.AdminCouponService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台券管理接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminCouponController {

    private final AdminCouponService adminCouponService;

    @GetMapping("/coupons")
    public Result<List<MerchantCouponVO>> list() {
        AdminGuard.requireAdmin();
        return Result.ok(adminCouponService.list());
    }

    @PostMapping("/coupons")
    public Result<Long> create(@Valid @RequestBody CouponSaveDTO dto) {
        AdminGuard.requireAdmin();
        return Result.ok(adminCouponService.save(null, dto));
    }

    @PutMapping("/coupons/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CouponSaveDTO dto) {
        AdminGuard.requireAdmin();
        adminCouponService.save(id, dto);
        return Result.ok();
    }

    @PutMapping("/coupons/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        AdminGuard.requireAdmin();
        adminCouponService.updateStatus(id, status);
        return Result.ok();
    }

    @DeleteMapping("/coupons/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        AdminGuard.requireAdmin();
        adminCouponService.delete(id);
        return Result.ok();
    }
}
