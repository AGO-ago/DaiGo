package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.ApplyApproveDTO;
import org.com.first.zhao_waimai.dto.ApplyRejectDTO;
import org.com.first.zhao_waimai.dto.MerchantApplyVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.service.AdminApplyService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台端入驻审核接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminApplyController {

    private final AdminApplyService adminApplyService;

    @GetMapping("/applies")
    public Result<PageDTO<MerchantApplyVO>> list(@RequestParam(required = false) Integer status,
                                                 @RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireAdmin();
        return Result.ok(adminApplyService.list(status, page, size));
    }

    @PutMapping("/apply/{id}/approve")
    public Result<Void> approve(@PathVariable Long id, @RequestBody(required = false) ApplyApproveDTO dto) {
        adminApplyService.approve(AdminGuard.requireAdmin(), id, dto);
        return Result.ok();
    }

    @PutMapping("/apply/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody ApplyRejectDTO dto) {
        adminApplyService.reject(AdminGuard.requireAdmin(), id, dto.getReason());
        return Result.ok();
    }
}
