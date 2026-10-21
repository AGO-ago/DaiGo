package org.com.first.zhao_waimai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置（来自 application.yml zhao-waimai.wx）
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhao-waimai.wx")
public class WxProperties {

    /** 小程序 AppID */
    private String appid;
    /** 小程序 AppSecret（生产从环境变量注入） */
    private String secret;
    /** 是否模拟登录（仅本地开发用测试号时开启） */
    private boolean mockEnabled = false;
}
