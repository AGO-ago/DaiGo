package org.com.first.zhao_waimai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 跨域配置（来自 application.yml zhao-waimai.cors）
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhao-waimai.cors")
public class CorsProperties {

    /**
     * 允许的跨域来源模式，支持端口通配（如 http://localhost:[*]）
     */
    private List<String> allowedOriginPatterns = List.of("http://localhost:[*]", "http://127.0.0.1:[*]");
}
