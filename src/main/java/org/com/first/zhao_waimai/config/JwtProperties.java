package org.com.first.zhao_waimai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置（来自 application.yml zhao-waimai.jwt）
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhao-waimai.jwt")
public class JwtProperties {

    /** 签名密钥（生产从环境变量注入） */
    private String secret;
    /** 有效期（秒） */
    private long expireSeconds = 7200;
}