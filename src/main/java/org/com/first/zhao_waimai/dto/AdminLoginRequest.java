package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 平台管理员登录请求
 */
@Data
public class AdminLoginRequest {

    /** 登录账号 */
    @NotBlank(message = "账号不能为空")
    private String username;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
