package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 领券中心券模板展示
 */
@Data
@Builder
public class CouponVO {

    /** 券模板 id */
    private Long id;

    /** 券名称 */
    private String name;

    /** 类型：1 满减 2 折扣 */
    private Integer type;

    /** 使用门槛 */
    private BigDecimal threshold;

    /** 减免额或折扣率 */
    private BigDecimal amount;

    /** 剩余数量 */
    private Integer remainCount;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 限定档口 id */
    private Long shopId;

    /** 限定档口名（空为平台通用） */
    private String shopName;

    /** 当前用户是否已领取 */
    private Boolean received;
}
