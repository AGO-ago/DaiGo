package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AdminShopVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.ShopCommissionDTO;
import org.com.first.zhao_waimai.service.AdminShopService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端档口管理接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminShopController {

    private final AdminShopService adminShopService;

    @GetMapping("/shops")
    public Result<PageDTO<AdminShopVO>> list(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) Integer status,
                                             @RequestParam(required = false) Long mallId,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireAdmin();
        return Result.ok(adminShopService.list(keyword, status, mallId, page, size));
    }

    @PutMapping("/shop/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        AdminGuard.requireAdmin();
        adminShopService.updateStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/shop/{id}/commission")
    public Result<Void> updateCommission(@PathVariable Long id,
                                         @Valid @RequestBody ShopCommissionDTO dto) {
        AdminGuard.requireAdmin();
        adminShopService.updateCommission(id, dto.getCommissionRate());
        return Result.ok();
    }
}
