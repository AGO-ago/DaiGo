package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.PublicConfigVO;

import java.math.BigDecimal;

/**
 * 平台配置统一读取入口（带本地缓存，后台保存后自动失效）
 */
public interface PlatformConfigService {

    /**
     * 读取字符串配置，空值返回默认值
     */
    String getString(String key, String defaultValue);

    /**
     * 读取整数配置，非法值返回默认值
     */
    int getInt(String key, int defaultValue);

    /**
     * 读取金额/比例配置，非法值返回默认值
     */
    BigDecimal getDecimal(String key, BigDecimal defaultValue);

    /**
     * 读取布尔开关配置（"1"/"true" 为真）
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 小程序可见的公开配置
     */
    PublicConfigVO publicView();

    /**
     * 配置变更后清空缓存
     */
    void invalidateCache();
}
