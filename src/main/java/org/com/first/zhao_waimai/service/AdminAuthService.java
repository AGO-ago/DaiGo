package org.com.first.zhao_waimai.service;

import org.com.first.zhao_waimai.dto.AdminLoginRequest;
import org.com.first.zhao_waimai.dto.AdminLoginResponse;
import org.com.first.zhao_waimai.dto.AdminProfileVO;

/**
 * 平台管理员鉴权服务
 */
public interface AdminAuthService {

    /**
     * 账号密码登录（连续 5 次失败锁定 10 分钟）
     */
    AdminLoginResponse login(AdminLoginRequest request);

    /**
     * 当前管理员信息
     */
    AdminProfileVO profile(Long adminId);
}
