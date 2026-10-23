package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.service.AdminConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 公开配置接口（无需登录）：小程序首页公告等展示信息
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class PublicConfigController {

    private final AdminConfigService adminConfigService;

    @GetMapping("/public")
    public Result<Map<String, String>> publicConfig() {
        return Result.ok(Map.of("homeNotice", adminConfigService.homeNotice()));
    }
}
