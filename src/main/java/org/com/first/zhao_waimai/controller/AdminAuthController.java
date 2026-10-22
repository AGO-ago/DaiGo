package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.AdminLoginRequest;
import org.com.first.zhao_waimai.dto.AdminLoginResponse;
import org.com.first.zhao_waimai.dto.AdminProfileVO;
import org.com.first.zhao_waimai.service.AdminAuthService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 平台管理员鉴权接口
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * 管理员登录（白名单接口）
     */
    @PostMapping("/auth/login")
    public Result<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.ok(adminAuthService.login(request));
    }

    /**
     * 当前管理员信息
     */
    @GetMapping("/profile")
    public Result<AdminProfileVO> profile() {
        Long adminId = AdminGuard.requireAdmin();
        return Result.ok(adminAuthService.profile(adminId));
    }
}
