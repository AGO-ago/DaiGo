package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.DashboardSummaryVO;
import org.com.first.zhao_waimai.dto.DashboardTrendVO;
import org.com.first.zhao_waimai.service.AdminDashboardService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台数据看板接口
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    /**
     * 今日/累计指标与待办
     */
    @GetMapping("/summary")
    public Result<DashboardSummaryVO> summary() {
        AdminGuard.requireAdmin();
        return Result.ok(adminDashboardService.summary());
    }

    /**
     * 近 N 天交易趋势
     */
    @GetMapping("/trend")
    public Result<List<DashboardTrendVO>> trend(@RequestParam(defaultValue = "7") int days) {
        AdminGuard.requireAdmin();
        return Result.ok(adminDashboardService.trend(days));
    }
}
