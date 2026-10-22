package org.com.first.zhao_waimai.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家端店铺券展示
 */
@Data
@Builder
public class MerchantCouponVO {

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

    /** 发放总量 */
    private Integer totalCount;

    /** 剩余数量 */
    private Integer remainCount;

    /** 已领取数量 */
    private Integer receivedCount;

    /** 生效时间 */
    private LocalDateTime startTime;

    /** 失效时间 */
    private LocalDateTime endTime;

    /** 状态：0 停用 1 启用 */
    private Integer status;
}
