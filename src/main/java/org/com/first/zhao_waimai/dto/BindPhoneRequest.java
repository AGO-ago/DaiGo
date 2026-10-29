package org.com.first.zhao_waimai.dto;

import lombok.Data;

/**
 * 绑定手机号请求
 */
@Data
public class BindPhoneRequest {

    /** 手机号（仅本地 mock 模式可直接传） */
    private String phone;

    /** 微信 getPhoneNumber 返回的动态令牌 */
    private String phoneCode;
}
