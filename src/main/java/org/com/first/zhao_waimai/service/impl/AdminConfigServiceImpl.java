package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.entity.PlatformConfig;
import org.com.first.zhao_waimai.mapper.PlatformConfigMapper;
import org.com.first.zhao_waimai.service.AdminConfigService;
import org.com.first.zhao_waimai.service.PlatformConfigService;
import org.com.first.zhao_waimai.util.AdminGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 平台系统配置实现
 */
@Service
@RequiredArgsConstructor
public class AdminConfigServiceImpl implements AdminConfigService {

    /** 客服电话：允许手机、座机、400 号码，仅数字与连字符 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9][0-9-]{4,19}$");
    /** 营业时间：HH:mm-HH:mm */
    private static final Pattern BUSINESS_HOURS_PATTERN =
            Pattern.compile("^([01]\\d|2[0-3]):[0-5]\\d-([01]\\d|2[0-3]):[0-5]\\d$");

    private final PlatformConfigMapper platformConfigMapper;
    private final PlatformConfigService platformConfigService;

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
        // 保存后立即失效配置快照，避免新配置要等缓存过期才生效
        platformConfigService.invalidateCache();
    }

    @Override
    public String homeNotice() {
        return platformConfigService.getString(PlatformConfig.KEY_HOME_NOTICE, "");
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
            case PlatformConfig.KEY_SUPPORT_PHONE -> {
                if (!PHONE_PATTERN.matcher(trimmed).matches()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "客服电话只能包含数字与连字符，长度 5-20 位");
                }
            }
            case PlatformConfig.KEY_BUSINESS_HOURS -> {
                if (!BUSINESS_HOURS_PATTERN.matcher(trimmed).matches()) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "营业时间格式需为 HH:mm-HH:mm，如 09:00-22:00");
                }
            }
            case PlatformConfig.KEY_PAY_TIMEOUT_MINUTES -> requireIntInRange(trimmed, 1, 120, "待支付超时分钟数");
            case PlatformConfig.KEY_COMMENT_DAYS -> requireIntInRange(trimmed, 1, 30, "评价时限天数");
            default -> {
                // 未知配置项按通用键值对保存
            }
        }
    }

    /**
     * 整数配置项范围校验
     */
    private void requireIntInRange(String value, int min, int max, String label) {
        int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "需为整数");
        }
        if (parsed < min || parsed > max) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, label + "需在 " + min + "-" + max + " 之间");
        }
    }
}
