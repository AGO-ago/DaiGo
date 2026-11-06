package org.com.first.zhao_waimai.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.dto.PublicConfigVO;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.mapper.PlatformConfigMapper;
import org.com.first.zhao_waimai.service.PlatformConfigService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 平台配置读取实现：整表快照 + 60 秒本地缓存，避免高频读配置打库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlatformConfigServiceImpl implements PlatformConfigService {

    /** 快照缓存有效期（毫秒） */
    private static final long CACHE_TTL_MILLIS = 60_000L;

    /** 默认配送费 */
    private static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("2.00");
    /** 默认起送价 */
    private static final BigDecimal DEFAULT_MIN_ORDER_AMOUNT = new BigDecimal("15.00");
    /** 默认待支付超时分钟数 */
    private static final int DEFAULT_PAY_TIMEOUT_MINUTES = 15;
    /** 默认评价时限（天） */
    private static final int DEFAULT_COMMENT_DAYS = 7;

    private final PlatformConfigMapper platformConfigMapper;

    private volatile Map<String, String> snapshot = Map.of();
    private volatile long expireAt;

    @Override
    public String getString(String key, String defaultValue) {
        String value = currentSnapshot().get(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    @Override
    public int getInt(String key, int defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 的值 {} 不是整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public BigDecimal getDecimal(String key, BigDecimal defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            log.warn("配置项 {} 的值 {} 不是数字，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue) {
        String value = getString(key, null);
        if (value == null) {
            return defaultValue;
        }
        return "1".equals(value) || "true".equalsIgnoreCase(value);
    }

    @Override
    public PublicConfigVO publicView() {
        return PublicConfigVO.builder()
                .homeNotice(getString(PlatformConfig.KEY_HOME_NOTICE, ""))
                .supportPhone(getString(PlatformConfig.KEY_SUPPORT_PHONE, ""))
                .businessHours(getString(PlatformConfig.KEY_BUSINESS_HOURS, ""))
                .deliveryFee(getDecimal(PlatformConfig.KEY_DELIVERY_FEE, DEFAULT_DELIVERY_FEE))
                .minOrderAmount(getDecimal(PlatformConfig.KEY_MIN_ORDER_AMOUNT, DEFAULT_MIN_ORDER_AMOUNT))
                .payTimeoutMinutes(getInt(PlatformConfig.KEY_PAY_TIMEOUT_MINUTES, DEFAULT_PAY_TIMEOUT_MINUTES))
                .commentDaysLimit(getInt(PlatformConfig.KEY_COMMENT_DAYS, DEFAULT_COMMENT_DAYS))
                .build();
    }

    @Override
    public void invalidateCache() {
        expireAt = 0L;
    }

    /**
     * 当前配置快照：过期后整表重载，加载失败时沿用旧快照
     */
    private Map<String, String> currentSnapshot() {
        if (System.currentTimeMillis() < expireAt) {
            return snapshot;
        }
        synchronized (this) {
            if (System.currentTimeMillis() < expireAt) {
                return snapshot;
            }
            try {
                List<PlatformConfig> configs = platformConfigMapper.selectList(null);
                Map<String, String> fresh = new HashMap<>();
                for (PlatformConfig config : configs) {
                    if (config.getConfigKey() != null && config.getConfigValue() != null) {
                        fresh.put(config.getConfigKey(), config.getConfigValue());
                    }
                }
                snapshot = Map.copyOf(fresh);
                expireAt = System.currentTimeMillis() + CACHE_TTL_MILLIS;
            } catch (Exception e) {
                // 读库异常时保留旧快照，避免配置读取拖垮业务
                log.error("平台配置加载失败，沿用上一次快照", e);
                expireAt = System.currentTimeMillis() + CACHE_TTL_MILLIS;
            }
            return snapshot;
        }
    }
}
