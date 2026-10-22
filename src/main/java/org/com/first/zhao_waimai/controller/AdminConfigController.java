package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.service.AdminConfigService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 平台系统配置接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminConfigController {

    private final AdminConfigService adminConfigService;

    @GetMapping("/configs")
    public Result<List<PlatformConfig>> list() {
        AdminGuard.requireAdmin();
        return Result.ok(adminConfigService.list());
    }

    @PutMapping("/configs")
    public Result<Void> update(@RequestBody Map<String, String> values) {
        AdminGuard.requireAdmin();
        adminConfigService.update(values);
        return Result.ok();
    }
}
