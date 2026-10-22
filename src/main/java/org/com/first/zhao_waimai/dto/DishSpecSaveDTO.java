package org.com.first.zhao_waimai.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 菜品规格保存请求
 */
@Data
public class DishSpecSaveDTO {

    /** 规格名 */
    @NotBlank(message = "规格名不能为空")
    @Size(max = 32, message = "规格名不能超过 32 个字符")
    private String name;

    /** 加价，为空按 0 处理 */
    @DecimalMin(value = "0", message = "规格加价不能为负数")
    private BigDecimal price;

    /** 排序值，越小越靠前 */
    private Integer sort;

    /** 状态：0 停用 1 启用 */
    private Integer status;
}
