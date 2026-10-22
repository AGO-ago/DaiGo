package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 平台管理员登录响应
 */
@Data
@Builder
public class AdminLoginResponse {

    /** 登录令牌 */
    private String token;

    /** 管理员 id */
    private Long adminId;

    /** 登录账号 */
    private String username;

    /** 姓名 */
    private String realName;

    /** 角色：1 超管 2 运营 */
    private Integer role;
}
