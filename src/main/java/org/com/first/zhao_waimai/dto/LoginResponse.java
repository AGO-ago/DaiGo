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

    /** 商家绑定的档口 id（网页端使用，其他角色为空） */
    private Long shopId;

    /** 商家绑定的档口名称（网页端使用，其他角色为空） */
    private String shopName;
}
