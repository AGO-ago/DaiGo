package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 店铺券保存请求（商家端/平台端共用）
 */
@Data
public class CouponSaveDTO {

    /** 券名称 */
    @NotBlank(message = "券名称不能为空")
    @Size(max = 64, message = "券名称不能超过 64 个字符")
    private String name;

    /** 类型：1 满减 2 折扣 */
    @NotNull(message = "券类型不能为空")
    @Min(value = 1, message = "券类型不合法")
    @Max(value = 2, message = "券类型不合法")
    private Integer type;

    /** 使用门槛金额 */
    @DecimalMin(value = "0", message = "门槛金额不能为负数")
    private BigDecimal threshold;

    /** 减免额（满减）或折扣率（折扣，如 0.85） */
    @NotNull(message = "优惠额度不能为空")
    @DecimalMin(value = "0.01", message = "优惠额度必须大于 0")
    private BigDecimal amount;

    /** 发放总量 */
    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为 1")
    private Integer totalCount;

    /** 生效时间 */
    @NotNull(message = "生效时间不能为空")
    private LocalDateTime startTime;

    /** 失效时间 */
    @NotNull(message = "失效时间不能为空")
    private LocalDateTime endTime;

    /** 状态：0 停用 1 启用 */
    private Integer status;
}
