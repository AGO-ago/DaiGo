package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.dto.AdminLoginRequest;
import org.com.first.zhao_waimai.dto.AdminLoginResponse;
import org.com.first.zhao_waimai.dto.AdminProfileVO;
import org.com.first.zhao_waimai.entity.Admin;
import org.com.first.zhao_waimai.mapper.AdminMapper;
import org.com.first.zhao_waimai.service.AdminAuthService;
import org.com.first.zhao_waimai.util.JwtUtil;
import org.com.first.zhao_waimai.util.PasswordUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 平台管理员鉴权实现：密码 PBKDF2 校验 + Redis 失败计数锁定
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    /** 最大连续失败次数 */
    private static final int MAX_FAIL_COUNT = 5;
    /** 锁定时长 */
    private static final Duration LOCK_DURATION = Duration.ofMinutes(10);
    /** 失败计数 key 前缀 */
    private static final String FAIL_KEY_PREFIX = "admin:login:fail:";

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        String username = request.getUsername().trim();
        String failKey = FAIL_KEY_PREFIX + username;
        String failCount = stringRedisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= MAX_FAIL_COUNT) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "登录失败次数过多，请 10 分钟后再试");
        }

        Admin admin = adminMapper.selectOne(Wrappers.<Admin>lambdaQuery()
                .eq(Admin::getUsername, username));
        // 统一提示，不区分账号是否存在
        if (admin == null || admin.getStatus() == null || admin.getStatus() == 0
                || !PasswordUtil.matches(request.getPassword(), admin.getSalt(), admin.getPassword())) {
            recordFailure(failKey);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号或密码错误");
        }

        stringRedisTemplate.delete(failKey);
        Admin update = new Admin();
        update.setId(admin.getId());
        update.setLastLoginTime(LocalDateTime.now());
        adminMapper.updateById(update);
        log.info("平台管理员登录成功, adminId={}", admin.getId());

        return AdminLoginResponse.builder()
                .token(jwtUtil.createToken(admin.getId(), CommonConstants.ROLE_ADMIN))
                .adminId(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .role(admin.getRole())
                .build();
    }

    @Override
    public AdminProfileVO profile(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "管理员不存在");
        }
        return AdminProfileVO.builder()
                .id(admin.getId())
                .username(admin.getUsername())
                .realName(admin.getRealName())
                .role(admin.getRole())
                .status(admin.getStatus())
                .lastLoginTime(admin.getLastLoginTime())
                .build();
    }

    /**
     * 记录失败次数，首次失败设置 10 分钟有效期
     */
    private void recordFailure(String failKey) {
        Long count = stringRedisTemplate.opsForValue().increment(failKey);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(failKey, LOCK_DURATION);
        }
    }
}
