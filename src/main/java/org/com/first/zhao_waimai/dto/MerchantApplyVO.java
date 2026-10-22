package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 入驻申请展示
 */
@Data
public class MerchantApplyVO {

    private Long id;

    private Long userId;

    private String userName;

    private String shopName;

    private Long mallId;

    private String mallName;

    private String contact;

    private String phone;

    private String licenseImg;

    private String businessHours;

    private Integer status;

    private String rejectReason;

    private Long shopId;

    private LocalDateTime auditTime;

    private LocalDateTime createTime;
}
