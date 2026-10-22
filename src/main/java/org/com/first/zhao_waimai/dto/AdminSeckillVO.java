package org.com.first.zhao_waimai.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 平台端秒杀活动展示
 */
@Data
public class AdminSeckillVO {

    /** 活动 id */
    private Long id;

    /** 菜品 id */
    private Long dishId;

    /** 菜品名称 */
    private String dishName;

    /** 菜品图 */
    private String dishImage;

    /** 菜品原价 */
    private BigDecimal dishPrice;

    /** 档口 id */
    private Long shopId;

    /** 档口名称 */
    private String shopName;

    /** 秒杀价 */
    private BigDecimal seckillPrice;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 活动库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer soldCount;

    /** 每人限购 */
    private Integer limitPerUser;

    /** 状态：0 下线 1 上线 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
