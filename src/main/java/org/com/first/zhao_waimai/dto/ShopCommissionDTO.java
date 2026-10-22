package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 档口抽佣比例设置（0-30%）
 */
@Data
public class ShopCommissionDTO {

    @NotNull(message = "抽佣比例不能为空")
    @DecimalMin(value = "0", message = "抽佣比例不能小于 0")
    @DecimalMax(value = "0.30", message = "抽佣比例不能超过 0.30")
    private BigDecimal commissionRate;
}
