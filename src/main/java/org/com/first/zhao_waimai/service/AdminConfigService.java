package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.entity.PlatformConfig;

import java.util.List;
import java.util.Map;

/**
 * 平台系统配置
 */
public interface AdminConfigService {

    /**
     * 全部配置项
     */
    List<PlatformConfig> list();

    /**
     * 批量保存配置项（键值对）
     */
    void update(Map<String, String> values);
}
