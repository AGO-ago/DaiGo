package org.com.first.zhao_waimai.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.first.zhao_waimai.common.BusinessException;
import org.com.first.zhao_waimai.common.CommonConstants;
import org.com.first.zhao_waimai.common.ErrorCode;
import org.com.first.zhao_waimai.config.WxProperties;
import org.com.first.zhao_waimai.dto.LoginRequest;
import org.com.first.zhao_waimai.dto.LoginResponse;
import org.com.first.zhao_waimai.entity.User;
import org.com.first.zhao_waimai.mapper.UserMapper;
import org.com.first.zhao_waimai.service.AuthService;
import org.com.first.zhao_waimai.util.JwtUtil;
import org.com.first.zhao_waimai.util.WxApiClient;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

/**
 * 鉴权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final WxProperties wxProperties;
    private final WxApiClient wxApiClient;

    @Override
    public LoginResponse login(LoginRequest request) {
        if (request.getRole() != CommonConstants.ROLE_USER
                && request.getRole() != CommonConstants.ROLE_MERCHANT) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法角色");
        }

        String openid = resolveOpenid(request.getCode());
        User user = upsertUser(openid);

        String token = jwtUtil.createToken(user.getId(), request.getRole());
        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .role(request.getRole())
                .build();
    }

    /**
     * 解析 openid：mock 开启时按 code 哈希生成稳定假 openid（测试号本地联调用）
     */
    private String resolveOpenid(String code) {
        if (wxProperties.isMockEnabled()) {
            return "mock-" + DigestUtils.md5DigestAsHex(code.getBytes(StandardCharsets.UTF_8));
        }
        return wxApiClient.code2session(code);
    }

    /**
     * 按 openid 查用户，不存在则注册
     */
    private User upsertUser(String openid) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getOpenid, openid));
        if (user != null) {
            if (user.getStatus() != null && user.getStatus() == 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号已被禁用");
            }
            return user;
        }
        user = new User();
        user.setOpenid(openid);
        user.setNickname("用户" + openid.substring(Math.max(0, openid.length() - 4)));
        user.setStatus(1);
        userMapper.insert(user);
        log.info("新用户注册, userId={}", user.getId());
        return user;
    }
}
