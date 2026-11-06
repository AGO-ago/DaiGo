package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 小程序可见的平台公开配置
 */
@Data
@Builder
public class PublicConfigVO {

    /** 首页公告 */
    private String homeNotice;

    /** 客服电话 */
    private String supportPhone;

    /** 营业时间（HH:mm-HH:mm） */
    private String businessHours;

    /** 默认配送费 */
    private BigDecimal deliveryFee;

    /** 默认起送价 */
    private BigDecimal minOrderAmount;

    /** 待支付超时分钟数（到点自动取消） */
    private Integer payTimeoutMinutes;

    /** 订单完成后的评价时限（天） */
    private Integer commentDaysLimit;
}
