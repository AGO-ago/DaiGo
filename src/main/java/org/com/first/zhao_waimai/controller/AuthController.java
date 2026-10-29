package org.com.first.zhao_waimai.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.BindPhoneRequest;
import org.com.first.zhao_waimai.dto.LoginRequest;
import org.com.first.zhao_waimai.dto.LoginResponse;
import org.com.first.zhao_waimai.dto.MerchantLoginRequest;
import org.com.first.zhao_waimai.dto.MerchantPasswordRequest;
import org.com.first.zhao_waimai.service.AuthService;
import org.com.first.zhao_waimai.util.UserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权接口
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录：微信 code 换 JWT
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(authService.login(request));
    }

    /**
     * 商家网页端登录：手机号 + 密码
     */
    @PostMapping("/merchant/login")
    public Result<LoginResponse> merchantLogin(@Valid @RequestBody MerchantLoginRequest request) {
        return Result.ok(authService.merchantLogin(request));
    }

    /**
     * 设置/修改商家网页登录密码
     */
    @PostMapping("/merchant/password")
    public Result<Void> setMerchantPassword(@Valid @RequestBody MerchantPasswordRequest request) {
        authService.setMerchantPassword(UserContext.getUserId(), request);
        return Result.ok();
    }

    /**
     * 是否已设置网页登录密码
     */
    @GetMapping("/merchant/password/status")
    public Result<Boolean> passwordStatus() {
        return Result.ok(authService.hasPassword(UserContext.getUserId()));
    }

    /**
     * 绑定手机号（微信一键授权 / 本地模拟）
     */
    @PostMapping("/phone")
    public Result<String> bindPhone(@RequestBody BindPhoneRequest request) {
        return Result.ok(authService.bindPhone(UserContext.getUserId(), request));
    }
}
