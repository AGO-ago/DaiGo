package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@Builder
public class LoginResponse {

    /** JWT 令牌 */
    private String token;

    /** 用户 id */
    private Long userId;

    /** 昵称 */
    private String nickname;

    /** 头像 URL */
    private String avatar;

    /** 手机号 */
    private String phone;

    /** 角色：1 用户 2 商家 */
    private Integer role;
}
