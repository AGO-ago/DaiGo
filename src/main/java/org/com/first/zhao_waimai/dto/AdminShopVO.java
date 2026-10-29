package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台端档口列表项
 */
@Data
public class AdminShopVO {

    private Long id;

    private Long mallId;

    private String mallName;

    private String name;

    private String logo;

    private String phone;

    private BigDecimal deliveryFee;

    private BigDecimal minOrderAmount;

    private Integer averageDeliveryTime;

    private String businessHours;

    private Integer status;

    private String announcement;

    private Integer autoAccept;

    private BigDecimal commissionRate;

    private Long merchantUserId;

    private String merchantName;

    /** 商家登录手机号（未授权时为空） */
    private String merchantPhone;

    /** 商家是否已设置网页登录密码 */
    private Boolean merchantHasPassword;

    private LocalDateTime createTime;
}
