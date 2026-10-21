package org.com.first.zhao_waimai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文件上传配置（本地磁盘起步，后续可换 OSS，接口不变）
 */
@Data
@Component
@ConfigurationProperties(prefix = "zhao-waimai.upload")
public class UploadProperties {

    /** 上传根目录 */
    private String dir = "./uploads";

    /** 允许的扩展名 */
    private String allowExts = "jpg,jpeg,png,webp,gif";
}
