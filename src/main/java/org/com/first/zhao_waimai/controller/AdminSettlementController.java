package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.dto.SettlementVO;
import org.com.first.zhao_waimai.service.AdminSettlementService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 平台端结算管理接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSettlementController {

    private final AdminSettlementService adminSettlementService;

    @PostMapping("/settlement/generate")
    public Result<Integer> generate(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AdminGuard.requireAdmin();
        return Result.ok(adminSettlementService.generate(date));
    }

    @GetMapping("/settlements")
    public Result<PageDTO<SettlementVO>> list(@RequestParam(required = false) Long shopId,
                                              @RequestParam(required = false) Integer status,
                                              @RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireAdmin();
        return Result.ok(adminSettlementService.list(shopId, status, page, size));
    }

    @PutMapping("/settlement/{id}/settle")
    public Result<Void> settle(@PathVariable Long id) {
        AdminGuard.requireAdmin();
        adminSettlementService.settle(id);
        return Result.ok();
    }
}
