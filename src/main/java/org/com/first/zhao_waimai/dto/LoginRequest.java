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
}
