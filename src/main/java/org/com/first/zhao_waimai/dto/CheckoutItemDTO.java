package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 购物车结算项（客户端购物车仅存 dishId + 数量，金额一律以后端为准）
 */
@Data
public class CheckoutItemDTO {

    /** 菜品 id */
    @NotNull(message = "菜品 id 不能为空")
    private Long dishId;

    /** 规格 id（无规格菜品传空） */
    private Long specId;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量至少为 1")
    @Max(value = 99, message = "单品数量不能超过 99")
    private Integer quantity;
}
