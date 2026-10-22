package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AdminOrderVO;
import org.com.first.zhao_waimai.dto.PageDTO;
import org.com.first.zhao_waimai.service.AdminOrderService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 平台端订单监控接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping("/orders")
    public Result<PageDTO<AdminOrderVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long shopId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        AdminGuard.requireAdmin();
        return Result.ok(adminOrderService.list(keyword, shopId, status, startDate, endDate, page, size));
    }

    @GetMapping("/order/{id}")
    public Result<AdminOrderVO> detail(@PathVariable Long id) {
        AdminGuard.requireAdmin();
        return Result.ok(adminOrderService.detail(id));
    }

    @PutMapping("/order/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        Long adminId = AdminGuard.requireAdmin();
        adminOrderService.cancel(adminId, id);
        return Result.ok();
    }
}
