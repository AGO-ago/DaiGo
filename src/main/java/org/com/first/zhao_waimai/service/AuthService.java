package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.BindPhoneRequest;
import org.com.first.zhao_waimai.dto.LoginRequest;
import org.com.first.zhao_waimai.dto.LoginResponse;
import org.com.first.zhao_waimai.dto.MerchantLoginRequest;
import org.com.first.zhao_waimai.dto.MerchantPasswordRequest;

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

    /**
     * 商家网页端登录（手机号 + 密码）
     *
     * @param request 登录请求
     * @return 令牌与商家信息
     */
    LoginResponse merchantLogin(MerchantLoginRequest request);

    /**
     * 设置/修改商家网页登录密码
     *
     * @param userId  当前登录用户
     * @param request 新旧密码
     */
    void setMerchantPassword(Long userId, MerchantPasswordRequest request);

    /**
     * 是否已设置网页登录密码
     */
    boolean hasPassword(Long userId);

    /**
     * 绑定手机号（微信一键授权或本地模拟）
     *
     * @param userId  当前登录用户
     * @param request 手机号 / 微信手机号令牌
     * @return 绑定后的手机号
     */
    String bindPhone(Long userId, BindPhoneRequest request);
}
