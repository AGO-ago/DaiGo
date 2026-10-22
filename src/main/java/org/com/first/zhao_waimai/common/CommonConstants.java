package org.com.first.zhao_waimai.common;

/**
 * 公共常量
 */
public final class CommonConstants {

    private CommonConstants() {
    }

    /** 请求头：登录令牌 */
    public static final String HEADER_TOKEN = "Authorization";
    /** 令牌前缀 */
    public static final String TOKEN_PREFIX = "Bearer ";

    /** 角色：用户 */
    public static final int ROLE_USER = 1;
    /** 角色：商家（档口） */
    public static final int ROLE_MERCHANT = 2;
    /** 角色：平台管理员 */
    public static final int ROLE_ADMIN = 3;

    /** 逻辑删除：未删除 */
    public static final int NOT_DELETED = 0;
}
