package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.mapper.PlatformConfigMapper;
import org.com.first.zhao_waimai.service.AdminConfigService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 平台系统配置实现
 */
@Service
@RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    private final PlatformConfigMapper platformConfigMapper;

    @Override
    public List<PlatformConfig> list() {
        AdminGuard.requireAdmin();
        return platformConfigMapper.selectList(Wrappers.<PlatformConfig>lambdaQuery()
                .orderByAsc(PlatformConfig::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Map<String, String> values) {
        AdminGuard.requireAdmin();
        if (values == null || values.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "配置内容不能为空");
        }
        values.forEach(this::validate);
        values.forEach((key, value) -> {
            PlatformConfig config = platformConfigMapper.selectOne(Wrappers.<PlatformConfig>lambdaQuery()
                    .eq(PlatformConfig::getConfigKey, key)
                    .last("LIMIT 1"));
            if (config == null) {
                config = new PlatformConfig();
                config.setConfigKey(key);
                config.setConfigValue(value == null ? "" : value);
                platformConfigMapper.insert(config);
            } else {
                PlatformConfig update = new PlatformConfig();
                update.setId(config.getId());
                update.setConfigValue(value == null ? "" : value);
                platformConfigMapper.updateById(update);
            }
        });
    }

    private void validate(String key, String value) {
        if (value == null) {
            return;
        }
        String trimmed = value.trim();
        switch (key) {
            case PlatformConfig.KEY_COMMISSION_RATE -> {
                BigDecimal rate;
                try {
                    rate = new BigDecimal(trimmed);
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "默认抽佣比例需为数字");
                }
                if (rate.compareTo(BigDecimal.ZERO) < 0
                        || rate.compareTo(new BigDecimal("0.30")) > 0) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "默认抽佣比例需在 0-0.30 之间");
                }
            }
            case PlatformConfig.KEY_DELIVERY_FEE, PlatformConfig.KEY_MIN_ORDER_AMOUNT -> {
                try {
                    if (new BigDecimal(trimmed).compareTo(BigDecimal.ZERO) < 0) {
                        throw new BusinessException(ErrorCode.PARAM_ERROR, "金额配置不能为负数");
                    }
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "金额配置需为数字");
                }
            }
            case PlatformConfig.KEY_AUTO_ACCEPT -> {
                if (!"0".equals(trimmed) && !"1".equals(trimmed)) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "自动接单开关仅支持 0 或 1");
                }
            }
            case PlatformConfig.KEY_HOME_NOTICE -> {
                if (trimmed.length() > 500) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "首页公告不能超过 500 字");
                }
            }
            default -> {
                // 未知配置项按通用键值对保存
            }
        }
    }
}
