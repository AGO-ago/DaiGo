package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商家网页端登录请求（手机号 + 密码）
 */
@Data
public class MerchantLoginRequest {

    /** 登录手机号 */
    @NotBlank(message = "手机号不能为空")
    private String phone;

    /** 登录密码 */
    @NotBlank(message = "密码不能为空")
    private String password;
}
