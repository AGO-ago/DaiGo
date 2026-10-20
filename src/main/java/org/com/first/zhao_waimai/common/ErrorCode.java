package org.com.first.zhao_waimai.common;

import lombok.Getter;

/**
 * 业务错误码
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已失效"),
    FORBIDDEN(403, "无权访问"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统异常"),
    BUSINESS_ERROR(1000, "业务处理失败");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
