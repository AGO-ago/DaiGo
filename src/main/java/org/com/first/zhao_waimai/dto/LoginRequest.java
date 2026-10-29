package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 登录请求
 */
@Data
public class LoginRequest {

    /** wx.login 获取的临时凭证 */
    @NotBlank(message = "code 不能为空")
    private String code;

    /** 角色：1 用户 2 商家 */
    @NotNull(message = "role 不能为空")
    private Integer role;

    /** 手机号（仅本地 mock 模式可直接传，生产由 phoneCode 换取） */
    private String phone;

    /** 微信 getPhoneNumber 返回的动态令牌 */
    private String phoneCode;

    /** 设备标识（仅本地 mock 模式用于稳定 openid，生产忽略） */
    private String deviceId;
}
