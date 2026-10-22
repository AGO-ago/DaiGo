package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 用户端秒杀活动展示
 */
@Data
public class SeckillVO {

    /** 活动 id */
    private Long id;

    /** 菜品 id */
    private Long dishId;

    /** 菜品名称 */
    private String name;

    /** 菜品图 */
    private String image;

    /** 秒杀价 */
    private BigDecimal price;

    /** 菜品原价 */
    private BigDecimal originalPrice;

    /** 档口 id */
    private Long shopId;

    /** 档口名称 */
    private String shopName;

    /** 美食城 id */
    private Long mallId;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 剩余可售数量 */
    private Integer remainStock;

    /** 每人限购 */
    private Integer limitPerUser;
}
