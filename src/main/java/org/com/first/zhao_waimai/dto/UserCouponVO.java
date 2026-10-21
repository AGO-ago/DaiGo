package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的优惠券
 */
@Data
@Builder
public class UserCouponVO {

    /** 用户券 id */
    private Long id;

    /** 券模板 id */
    private Long couponId;

    /** 券名称 */
    private String name;

    /** 类型：1 满减 2 折扣 */
    private Integer type;

    /** 使用门槛 */
    private BigDecimal threshold;

    /** 减免额或折扣率 */
    private BigDecimal amount;

    /** 限定档口 id */
    private Long shopId;

    /** 限定档口名 */
    private String shopName;

    /** 状态：0 未使用 1 已使用 */
    private Integer status;

    /** 失效时间 */
    private LocalDateTime endTime;
}
