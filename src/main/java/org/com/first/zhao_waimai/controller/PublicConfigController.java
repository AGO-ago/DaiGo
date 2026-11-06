package org.com.first.zhao_waimai.controller;

import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.Result;
import org.com.first.zhao_waimai.dto.PublicConfigVO;
import org.com.first.zhao_waimai.service.PlatformConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开配置接口（无需登录）：小程序首页公告、客服电话、营业时间等展示信息
 */
@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
public class PublicConfigController {

    private final PlatformConfigService platformConfigService;

    @GetMapping("/public")
    public Result<PublicConfigVO> publicConfig() {
        return Result.ok(platformConfigService.publicView());
    }
}
