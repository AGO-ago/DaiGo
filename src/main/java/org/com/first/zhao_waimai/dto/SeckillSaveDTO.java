package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀活动保存入参
 */
@Data
public class SeckillSaveDTO {

    /** 菜品 id */
    @NotNull(message = "请选择菜品")
    private Long dishId;

    /** 秒杀价 */
    @NotNull(message = "请输入秒杀价")
    @DecimalMin(value = "0.01", message = "秒杀价需大于 0")
    private BigDecimal seckillPrice;

    /** 开始时间 */
    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    /** 结束时间 */
    @NotNull(message = "请选择结束时间")
    private LocalDateTime endTime;

    /** 活动库存 */
    @NotNull(message = "请输入活动库存")
    @Min(value = 0, message = "活动库存不能为负")
    private Integer stock;

    /** 每人限购份数 */
    @NotNull(message = "请输入每人限购份数")
    @Min(value = 1, message = "每人限购至少 1 份")
    private Integer limitPerUser;

    /** 状态：0 下线 1 上线 */
    private Integer status;
}
