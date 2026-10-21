package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.LoginRequest;
import org.com.first.zhao_waimai.dto.LoginResponse;

/**
 * 鉴权服务
 */
public interface AuthService {

    /**
     * 登录（微信 code 换 openid，落库并发 JWT）
     *
     * @param request 登录请求
     * @return 令牌与用户信息
     */
    LoginResponse login(LoginRequest request);
}
