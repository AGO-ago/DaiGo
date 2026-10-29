package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 商家设置/修改网页登录密码
 */
@Data
public class MerchantPasswordRequest {

    /** 原密码（首次设置时可为空） */
    private String oldPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度需为 6-20 位")
    private String newPassword;
}
